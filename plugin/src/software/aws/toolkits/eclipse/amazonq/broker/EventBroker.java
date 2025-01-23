// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.broker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import software.aws.toolkits.eclipse.amazonq.observers.EventObserver;
import software.aws.toolkits.eclipse.amazonq.observers.StreamObserver;

public final class EventBroker {

    @FunctionalInterface
    private interface TypedCallable<T> {
        void callWith(T event);
    }

    public static final class CallerRunsPolicyBlocking implements RejectedExecutionHandler {

        private final BlockingQueue<Runnable> workQueue;

        CallerRunsPolicyBlocking(final BlockingQueue<Runnable> workQueue) {
            this.workQueue = workQueue;
        }

        @Override
        public void rejectedExecution(final Runnable runnable, final ThreadPoolExecutor executor) {
            if (!executor.isShutdown()) {
                try {
                    workQueue.put(runnable);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new RejectedExecutionException("Task " + runnable + " rejected from " + executor, exception);
                }
            } else {
                throw new RejectedExecutionException("Task " + runnable + " rejected from " + executor);
            }
        }

    }

    public static final class OrderedThreadPoolExecutor {

        private final Map<String, BlockingQueue<?>> bufferedEventsForInterest;
        private final Map<String, AtomicBoolean> jobStatusForInterest;
        private final Map<String, TypedCallable<?>> callbackForInterest;
        private final Map<String, Object> lastEventForInterest;

        private final BlockingQueue<Runnable> scheduledJobs;
        private final ThreadPoolExecutor executor;
        private final int eventQueueCapacity;

        public static final int EVENT_BATCH_SIZE = 250;

        OrderedThreadPoolExecutor(final int coreThreadCount, final int maxThreadCount, final int jobQueueCapacity,
                final int eventQueueCapacity, final int keepAliveTime, final TimeUnit keepAliveTimeUnit) {
            scheduledJobs = new ArrayBlockingQueue<>(jobQueueCapacity);
            bufferedEventsForInterest = new ConcurrentHashMap<>();
            jobStatusForInterest = new ConcurrentHashMap<>();
            callbackForInterest = new ConcurrentHashMap<>();
            lastEventForInterest = new ConcurrentHashMap<>();

            this.eventQueueCapacity = eventQueueCapacity;

            executor = new ThreadPoolExecutor(coreThreadCount, maxThreadCount, keepAliveTime, keepAliveTimeUnit,
                    scheduledJobs, Executors.defaultThreadFactory(), new CallerRunsPolicyBlocking(scheduledJobs));
        }

        public <T> void registerCallbackForInterest(final String interestId, final TypedCallable<T> callback) {
            callbackForInterest.putIfAbsent(interestId, callback);
        }

        public boolean isCallbackRegisteredForInterest(final String interestId) {
            return callbackForInterest.containsKey(interestId);
        }

        @SuppressWarnings("unchecked")
        public <T> void submitEventForInterest(final String interestId, final T event) {
            BlockingQueue<T> bufferedEvents = (BlockingQueue<T>) bufferedEventsForInterest.computeIfAbsent(interestId,
                    k -> new ArrayBlockingQueue<>(eventQueueCapacity, true));
            try {
                bufferedEvents.put(event);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            handleJobScheduling(interestId, (Class<T>) event.getClass(), bufferedEvents);
        }

        private <T> void handleJobScheduling(final String interestId, final Class<T> eventType,
                final BlockingQueue<T> bufferedEvents) {
            AtomicBoolean jobStatus = jobStatusForInterest.computeIfAbsent(interestId, k -> new AtomicBoolean(false));

            if (jobStatus.compareAndSet(false, true)) {
                executor.submit(() -> processQueuedEvents(interestId, eventType, bufferedEvents, jobStatus));
            }
        }

        @SuppressWarnings("unchecked")
        private <T> void processQueuedEvents(final String interestId, final Class<T> eventType,
                final BlockingQueue<T> bufferedEvents, final AtomicBoolean jobStatus) {
            try {
                TypedCallable<T> eventCallback = (TypedCallable<T>) callbackForInterest.get(interestId);
                if (eventCallback == null) {
                    return;
                }

                List<T> batchedEvents = new ArrayList<>(EVENT_BATCH_SIZE);
                T lastEvent = Optional.ofNullable(lastEventForInterest.get(interestId)).map(event -> (T) event)
                        .orElse(null);

                while (bufferedEvents.drainTo(batchedEvents, EVENT_BATCH_SIZE) > 0) {
                    for (T newEvent : batchedEvents) {
                        try {
                            if (!newEvent.equals(lastEvent)) {
                                eventCallback.callWith(newEvent);
                            }
                            lastEvent = newEvent;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    batchedEvents.clear();
                }

                lastEventForInterest.put(interestId, lastEvent);
            } finally {
                jobStatus.set(false);
            }
        }

    }

    private static final EventBroker INSTANCE;
    private final Map<Class<?>, SubmissionPublisher<?>> publisherForEventType;
    private final OrderedThreadPoolExecutor publisherExecutor;
    private final OrderedThreadPoolExecutor subscriberExecutor;

    static {
        INSTANCE = new EventBroker();
    }

    private EventBroker() {
        publisherForEventType = new ConcurrentHashMap<>();
        publisherExecutor = new OrderedThreadPoolExecutor(5, 20, 50, 100, 10, TimeUnit.MILLISECONDS);
        subscriberExecutor = new OrderedThreadPoolExecutor(5, 20, 50, 100, 10, TimeUnit.MILLISECONDS);
    }

    public static EventBroker getInstance() {
        return INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public <T> void post(final T event) {
        if (event == null) {
            return;
        }

        SubmissionPublisher<T> publisher = getPublisherForEventType((Class<T>) event.getClass());
        if (!publisherExecutor.isCallbackRegisteredForInterest((event.getClass().getName()))) {
            registerPublisherCallbackForInterest(event.getClass().getName(), publisher);
        }

        publisherExecutor.submitEventForInterest(event.getClass().getName(), event);
    }

    public <T> Subscription subscribe(final EventObserver<T> observer) {
        SubmissionPublisher<T> publisher = getPublisherForEventType(observer.getEventType());
        AtomicReference<Subscription> subscriptionReference = new AtomicReference<>();
        String subscriberId = UUID.randomUUID().toString();

        registerSubscriberCallbackForInterest(subscriberId, observer);

        Subscriber<T> subscriber = new Subscriber<>() {

            private volatile Subscription subscription;

            @Override
            public void onSubscribe(final Subscription subscription) {
                this.subscription = subscription;
                subscriptionReference.set(subscription);
                this.subscription.request(1);
            }

            @Override
            public void onNext(final T event) {
                subscriberExecutor.submitEventForInterest(subscriberId, event);
                subscription.request(1);
            }

            @Override
            public void onError(final Throwable error) {
                error.printStackTrace();
            }

            @Override
            public void onComplete() {
                return;
            }

        };

        publisher.subscribe(subscriber);
        return subscriptionReference.get();
    }

    public <T> Subscription subscribe(final StreamObserver<T> observer) {
        SubmissionPublisher<T> publisher = getPublisherForEventType(observer.getEventType());
        AtomicReference<Subscription> subscriptionReference = new AtomicReference<>();
        String subscriberId = UUID.randomUUID().toString();

        registerSubscriberCallbackForInterest(subscriberId, observer);

        Subscriber<T> subscriber = new Subscriber<>() {

            private volatile Subscription subscription;

            @Override
            public void onSubscribe(final Subscription subscription) {
                this.subscription = subscription;
                subscriptionReference.set(subscription);
                this.subscription.request(1);
            }

            @Override
            public void onNext(final T event) {
                subscriberExecutor.submitEventForInterest(subscriberId, event);
                subscription.request(1);
            }

            @Override
            public void onError(final Throwable error) {
                observer.onError(error);
            }

            @Override
            public void onComplete() {
                observer.onComplete();
            }

        };

        publisher.subscribe(subscriber);
        return subscriptionReference.get();
    }

    @SuppressWarnings("unchecked")
    private <T> SubmissionPublisher<T> getPublisherForEventType(final Class<T> eventType) {
        return (SubmissionPublisher<T>) publisherForEventType.computeIfAbsent(eventType,
                key -> new SubmissionPublisher<>(Runnable::run, Flow.defaultBufferSize()));
    }

    private <T> void registerSubscriberCallbackForInterest(final String interestId,
            final EventObserver<T> observer) {
        TypedCallable<T> eventCallback = new TypedCallable<>() {
            @Override
            public void callWith(final T event) {
                observer.onEvent(event);
            }
        };
        subscriberExecutor.registerCallbackForInterest(interestId, eventCallback);
    }

    private <T> void registerPublisherCallbackForInterest(final String interestId,
            final SubmissionPublisher<T> publisher) {
        TypedCallable<T> eventCallback = new TypedCallable<>() {
            @Override
            public void callWith(final T event) {
                publisher.submit(event);
            }
        };
        publisherExecutor.registerCallbackForInterest(interestId, eventCallback);
    }

}
