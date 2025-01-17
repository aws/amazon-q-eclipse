// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.broker;

import java.util.Map;
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
import java.util.concurrent.locks.ReentrantLock;

import software.aws.toolkits.eclipse.amazonq.observers.EventObserver;
import software.aws.toolkits.eclipse.amazonq.observers.StreamObserver;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

public final class EventBroker {

    @FunctionalInterface
    private interface TypedCallable<T> {
        void call(T event);
    }

    private final class BlockingCallerRunsPolicy implements RejectedExecutionHandler {

        private final BlockingQueue<Runnable> workQueue;

        BlockingCallerRunsPolicy(final BlockingQueue<Runnable> workQueue) {
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

    private class OrderedThreadPoolExecutor {

        private final Map<String, BlockingQueue<?>> typedEventQueue;
        private final Map<String, AtomicBoolean> typedJobStatus;
        private final Map<String, ReentrantLock> typedJobLock;
        private final Map<String, TypedCallable<?>> typedCallback;

        private final BlockingQueue<Runnable> workQueue;
        private final ThreadPoolExecutor executor;
        private final int eventQueueCapacity;

        OrderedThreadPoolExecutor(final int coreThreadCount, final int maxThreadCount, final int queueCapacity,
                final int keepAliveTime, final int eventQueueCapacity) {
            workQueue = new ArrayBlockingQueue<>(queueCapacity);
            typedEventQueue = new ConcurrentHashMap<>();
            typedJobStatus = new ConcurrentHashMap<>();
            typedJobLock = new ConcurrentHashMap<>();
            typedCallback = new ConcurrentHashMap<>();

            this.eventQueueCapacity = eventQueueCapacity;

            executor = new ThreadPoolExecutor(coreThreadCount, maxThreadCount, keepAliveTime, TimeUnit.MILLISECONDS,
                    workQueue, Executors.defaultThreadFactory(), new BlockingCallerRunsPolicy(workQueue));
        }

        public <T, R> void registerCallback(final String interestId, final TypedCallable<R> callback) {
            typedCallback.putIfAbsent(interestId, callback);
        }

        public <T> boolean hasRegisteredCallback(final String interestType) {
            return typedCallback.containsKey(interestType);
        }

        @SuppressWarnings("unchecked")
        public <T, R> void submit(final String interestId, final R event) {
            BlockingQueue<R> eventQueue = (BlockingQueue<R>) typedEventQueue.computeIfAbsent(interestId,
                    k -> new ArrayBlockingQueue<>(eventQueueCapacity, true));
            try {
                eventQueue.put(event);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            handleScheduling(interestId, (Class<R>) event.getClass(), eventQueue);
        }

        public <T, R> void handleScheduling(final String interestId, final Class<R> eventType,
                final BlockingQueue<R> eventQueue) {
            AtomicBoolean jobStatus = typedJobStatus.computeIfAbsent(interestId, k -> new AtomicBoolean(false));
            ReentrantLock jobLock = typedJobLock.computeIfAbsent(interestId, k -> new ReentrantLock(true));

            jobLock.lock();
            try {
                if (!jobStatus.get() && !eventQueue.isEmpty()) {
                    if (jobStatus.compareAndSet(false, true)) {
                        executor.submit(() -> processEventQueue(interestId, eventType,
                                eventQueue, jobStatus, jobLock));
                    }
                }
            } finally {
                jobLock.unlock();
            }
        }

        @SuppressWarnings("unchecked")
        public <T, R> void processEventQueue(final String interestId, final Class<R> eventType,
                final BlockingQueue<R> eventQueue, final AtomicBoolean jobStatus, final ReentrantLock jobLock) {
            if (jobStatus == null || jobLock == null || eventQueue == null) {
                throw new NullPointerException("ThreadPoolExecutor in unexpected state");
            }

            jobLock.lock();
            try {
                TypedCallable<R> eventCallback = (TypedCallable<R>) typedCallback.get(interestId);
                if (eventCallback == null) {
                    return;
                }

                while (!eventQueue.isEmpty()) {
                    try {
                        R newEvent = eventQueue.take();
                        if (newEvent != null) {
                            try {
                                eventCallback.call(newEvent);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } finally {
                try {
                    jobStatus.set(false);
                } finally {
                    jobLock.unlock();
                }
            }
        }
    }

    private static final EventBroker INSTANCE;
    private final Map<Class<?>, SubmissionPublisher<?>> publishers;
    private final OrderedThreadPoolExecutor emissionExecutor;
    private final OrderedThreadPoolExecutor consumptionExecutor;

    static {
        INSTANCE = new EventBroker();
    }

    private EventBroker() {
        publishers = new ConcurrentHashMap<>();

        emissionExecutor = new OrderedThreadPoolExecutor(5, 30, 30, 10, 100000000);
        consumptionExecutor = new OrderedThreadPoolExecutor(5, 30, 30, 10, 100000000);
    }

    public static EventBroker getInstance() {
        return INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public <T> void post(final T event) {
        if (event == null) {
            return;
        }

        SubmissionPublisher<T> publisher = getPublisher((Class<T>) event.getClass());
        if (!emissionExecutor.hasRegisteredCallback((event.getClass().getName()))) {
            registerPublisherCallback(publisher, event.getClass().getName());
        }

        emissionExecutor.submit(event.getClass().getName(), event);
    }

    public <T> Subscription subscribe(final EventObserver<T> observer) {
        SubmissionPublisher<T> publisher = getPublisher(observer.getEventType());
        AtomicReference<Subscription> subscriptionReference = new AtomicReference<>();
        String subscriberId = UUID.randomUUID().toString();

        registerSubscriberCallback(observer, subscriberId);

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
                consumptionExecutor.submit(subscriberId, event);
                this.subscription.request(1);
            }

            @Override
            public void onError(final Throwable throwable) {
                return;
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
        SubmissionPublisher<T> publisher = getPublisher(observer.getEventType());
        AtomicReference<Subscription> subscriptionReference = new AtomicReference<>();
        String subscriberId = UUID.randomUUID().toString();

        registerSubscriberCallback(observer, subscriberId);

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
                consumptionExecutor.submit(subscriberId, event);
                this.subscription.request(1);
            }

            @Override
            public void onError(final Throwable throwable) {
                observer.onError(throwable);
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
    private <T> SubmissionPublisher<T> getPublisher(final Class<T> eventType) {
        return (SubmissionPublisher<T>) publishers.computeIfAbsent(eventType,
                key -> new SubmissionPublisher<>(Runnable::run, Flow.defaultBufferSize()));
    }

    private <T> void registerSubscriberCallback(final EventObserver<T> subscriber, final String subscriberId) {
        Activator.getLogger().info(subscriberId);
        TypedCallable<T> eventCallback = new TypedCallable<>() {
            @Override
            public void call(final T event) {
                subscriber.onEvent(event);
            }
        };
        consumptionExecutor.registerCallback(subscriberId, eventCallback);
    }

    private <T> void registerPublisherCallback(final SubmissionPublisher<T> publisher, final String eventId) {
        TypedCallable<T> eventCallback = new TypedCallable<>() {
            @Override
            public void call(final T event) {
                publisher.submit(event);
            }
        };
        emissionExecutor.registerCallback(eventId, eventCallback);
    }

}
