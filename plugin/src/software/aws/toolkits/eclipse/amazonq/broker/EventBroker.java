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

        private final Map<String, BlockingQueue<?>> interestIdToEventQueueMap;
        private final Map<String, AtomicBoolean> interestIdToJobStatusMap;
        private final Map<String, TypedCallable<?>> interestIdToCallbackMap;
        private final Map<String, Object> interestIdToLastEventMap;

        private final BlockingQueue<Runnable> scheduledJobsQueue;
        private final ThreadPoolExecutor executor;
        private final int eventQueueCapacity;

        public static final int EVENT_BATCH_SIZE = 250;

        OrderedThreadPoolExecutor(final int coreThreadCount, final int maxThreadCount, final int jobQueueCapacity,
                final int eventQueueCapacity, final int keepAliveTime, final TimeUnit keepAliveTimeUnit) {
            scheduledJobsQueue = new ArrayBlockingQueue<>(jobQueueCapacity);
            interestIdToEventQueueMap = new ConcurrentHashMap<>();
            interestIdToJobStatusMap = new ConcurrentHashMap<>();
            interestIdToCallbackMap = new ConcurrentHashMap<>();
            interestIdToLastEventMap = new ConcurrentHashMap<>();

            this.eventQueueCapacity = eventQueueCapacity;

            executor = new ThreadPoolExecutor(coreThreadCount, maxThreadCount, keepAliveTime, keepAliveTimeUnit,
                    scheduledJobsQueue, Executors.defaultThreadFactory(), new CallerRunsPolicyBlocking(scheduledJobsQueue));
        }

        public <T, R> void registerCallbackForInterest(final String interestId, final TypedCallable<R> callback) {
            interestIdToCallbackMap.putIfAbsent(interestId, callback);
        }

        public <T> boolean isCallbackRegisteredForInterest(final String interestId) {
            return interestIdToCallbackMap.containsKey(interestId);
        }

        @SuppressWarnings("unchecked")
        public <T, R> void submitEventForInterest(final String interestId, final R event) {
            BlockingQueue<R> eventQueue = (BlockingQueue<R>) interestIdToEventQueueMap.computeIfAbsent(interestId,
                    k -> new ArrayBlockingQueue<>(eventQueueCapacity, true));
            try {
                eventQueue.put(event);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            handleJobScheduling(interestId, (Class<R>) event.getClass(), eventQueue);
        }

        private <T, R> void handleJobScheduling(final String interestId, final Class<R> eventType,
                final BlockingQueue<R> eventQueue) {
            AtomicBoolean jobStatus = interestIdToJobStatusMap.computeIfAbsent(interestId, k -> new AtomicBoolean(false));

            if (jobStatus.compareAndSet(false, true)) {
                executor.submit(() -> processQueuedEvents(interestId, eventType, eventQueue, jobStatus));
            }
        }

        @SuppressWarnings("unchecked")
        private <T, R> void processQueuedEvents(final String interestId, final Class<R> eventType,
                final BlockingQueue<R> eventQueue, final AtomicBoolean jobStatus) {
            try {
                TypedCallable<R> eventCallback = (TypedCallable<R>) interestIdToCallbackMap.get(interestId);
                if (eventCallback == null) {
                    return;
                }

                List<R> eventBatchQueue = new ArrayList<>(EVENT_BATCH_SIZE);
                R lastEvent = Optional.ofNullable(interestIdToLastEventMap.get(interestId)).map(event -> (R) event)
                        .orElse(null);

                while (eventQueue.drainTo(eventBatchQueue, EVENT_BATCH_SIZE) > 0) {
                    for (R newEvent : eventBatchQueue) {
                        try {
                            if (!newEvent.equals(lastEvent)) {
                                eventCallback.callWith(newEvent);
                            }
                            lastEvent = newEvent;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    eventBatchQueue.clear();
                }

                interestIdToLastEventMap.put(interestId, lastEvent);
            } finally {
                jobStatus.set(false);
            }
        }

    }

    private static final EventBroker INSTANCE;
    private final Map<Class<?>, SubmissionPublisher<?>> eventTypeToPublisherMap;
    private final OrderedThreadPoolExecutor publisherExecutor;
    private final OrderedThreadPoolExecutor subscriberExecutor;

    static {
        INSTANCE = new EventBroker();
    }

    private EventBroker() {
        eventTypeToPublisherMap = new ConcurrentHashMap<>();
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
        return (SubmissionPublisher<T>) eventTypeToPublisherMap.computeIfAbsent(eventType,
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
