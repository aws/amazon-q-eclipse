// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.broker;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import software.aws.toolkits.eclipse.amazonq.subscriber.Subscriber;

public final class EventBroker {

    @FunctionalInterface
    private interface TypedCallable<T> {
        void runCallback(T event);
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

        private final Map<Class<?>, BlockingQueue<?>> typedEventQueue;
        private final Map<Class<?>, AtomicBoolean> typedJobStatus;
        private final Map<Class<?>, ReentrantLock> typedJobLock;
        private final Map<Class<?>, TypedCallable<?>> typedCallback;

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

        @SuppressWarnings("unchecked")
        public <T, R> void submit(final Class<T> interestType, final R event) {
            BlockingQueue<R> eventQueue = (BlockingQueue<R>) typedEventQueue.computeIfAbsent(interestType,
                    k -> new ArrayBlockingQueue<>(eventQueueCapacity));
            eventQueue.offer(event);

            handleScheduling(interestType, event.getClass());
        }

        public <T, R> void registerCallback(final Class<T> interestType, final TypedCallable<R> callback) {
            typedCallback.putIfAbsent(interestType, callback);
        }

        public <T, R> void handleScheduling(final Class<T> interestType, final Class<R> eventType) {
            AtomicBoolean jobStatus = typedJobStatus.computeIfAbsent(interestType, k -> new AtomicBoolean(false));
            ReentrantLock jobLock = typedJobLock.computeIfAbsent(interestType, k -> new ReentrantLock());

            if (!jobStatus.get()) {
                jobLock.lock();

                try {
                    if (jobStatus.compareAndSet(false, true)) {
                        processEventQueue(interestType, eventType);
                    }
                } finally {
                    jobLock.unlock();
                }
            }
        }

        @SuppressWarnings("unchecked")
        public <T, R> void processEventQueue(final Class<T> interestType, final Class<R> eventType) {
            TypedCallable<R> eventCallback = (TypedCallable<R>) typedCallback.get(interestType);
            if (eventCallback == null) {
                return;
            }

            AtomicBoolean jobStatus = typedJobStatus.get(interestType);
            ReentrantLock jobLock = typedJobLock.get(interestType);
            BlockingQueue<R> eventQueue = (BlockingQueue<R>) typedEventQueue.get(interestType);

            if (jobStatus == null || jobLock == null || eventQueue == null) {
                throw new NullPointerException("ThreadPoolExecutor in unexpected state.");
            }

            R newEvent = eventQueue.poll();
            jobLock.lock();

            if (newEvent != null) {
                boolean hasJobStatusUpdated = jobStatus.compareAndSet(true, !eventQueue.isEmpty());
                eventCallback.runCallback(newEvent);

                if (!hasJobStatusUpdated) {
                    processEventQueue(interestType, eventType);
                }
            } else {
                jobStatus.set(false);
            }

            jobLock.unlock();
        }

    }

    private static final EventBroker INSTANCE;
    private final Map<Class<?>, SubmissionPublisher<?>> publishers;
    private final OrderedThreadPoolExecutor emissionExecutor;
    private final OrderedThreadPoolExecutor consumptionExecutor;
    private final Set<Class<?>> publishEventCallbacksRegistered;

    static {
        INSTANCE = new EventBroker();
    }

    private EventBroker() {
        publishers = new ConcurrentHashMap<>();
        publishEventCallbacksRegistered = ConcurrentHashMap.newKeySet();

        emissionExecutor = new OrderedThreadPoolExecutor(10, 10, 10, 10, 10);
        consumptionExecutor = new OrderedThreadPoolExecutor(10, 10, 10, 10, 10);
    }

    public static EventBroker getInstance() {
        return INSTANCE;
    }

    public <T> void post(final T event) {
        if (event == null) {
            return;
        }

        @SuppressWarnings("unchecked")
        SubmissionPublisher<T> publisher = getPublisher((Class<T>) event.getClass());

        if (!publishEventCallbacksRegistered.contains(event.getClass())) {
            TypedCallable<T> eventCallback = new TypedCallable<>() {
                @Override
                public void runCallback(final T event) {
                    publisher.submit(event);
                }
            };

            emissionExecutor.registerCallback(event.getClass(), eventCallback);
        }

        emissionExecutor.submit(event.getClass(), event);
    }

    public <T> Subscription subscribe(final Subscriber<T> subscriber) {
        SubmissionPublisher<T> publisher = getPublisher(subscriber.getEventType());
        AtomicReference<Subscription> subscriptionReference = new AtomicReference<>();
        Class<?> customSubscriberToken = createUniqueSubscriberClassToken();

        TypedCallable<T> eventCallback = new TypedCallable<>() {
            @Override
            public void runCallback(final T event) {
                subscriber.handleEvent(event);
            }
        };
        consumptionExecutor.registerCallback(customSubscriberToken, eventCallback);

        java.util.concurrent.Flow.Subscriber<T> subscriberWrapper = new java.util.concurrent.Flow.Subscriber<>() {

            private java.util.concurrent.Flow.Subscription subscription;
            private Class<?> subscriberToken = customSubscriberToken;

            @Override
            public void onSubscribe(final java.util.concurrent.Flow.Subscription subscription) {
                this.subscription = subscription;
                subscriptionReference.set(subscription);

                this.subscription.request(1);
            }

            @Override
            public void onNext(final T event) {
                consumptionExecutor.submit(subscriberToken, event);
                this.subscription.request(1);
            }

            @Override
            public void onError(final Throwable throwable) {
                subscriber.handleError(throwable);
            }

            @Override
            public void onComplete() {
                // TODO: add if required
            }

        };

        publisher.subscribe(subscriberWrapper);
        return subscriptionReference.get();
    }

    @SuppressWarnings("unchecked")
    private <T> SubmissionPublisher<T> getPublisher(final Class<T> eventType) {
        return (SubmissionPublisher<T>) publishers.computeIfAbsent(eventType,
                key -> new SubmissionPublisher<>(Executors.newSingleThreadExecutor(), Flow.defaultBufferSize()));
    }

    private Class<?> createUniqueSubscriberClassToken() {
        return new Object() {
            private static final String ID = UUID.randomUUID().toString();
        }.getClass();
    }
}
