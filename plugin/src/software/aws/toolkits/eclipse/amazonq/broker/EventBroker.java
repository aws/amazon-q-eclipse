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


/**
 * A thread-safe event broker that implements the publish-subscribe pattern for asynchronous event handling.
 * This singleton class manages event publication and subscription using a concurrent execution model.
 *
 * The broker provides:
 * - Thread-safe event publishing and subscription
 * - Ordered event processing with batching support
 * - Configurable thread pools for publishers and subscribers
 * - De-duplication of consecutive identical events
 * 
 * Thread Safety: This implementation is thread-safe and can handle concurrent publications
 * and subscriptions from multiple threads.
 *
 * Example usage:
 * EventBroker broker = EventBroker.getInstance();
 *
 * // Subscribe to events
 * EventObserver<MyEvent> observer = new EventObserver<>() {
 *      @Override
 *      public void onEvent(final MyEvent event) {
 *              // handle event
 *      }
 * };
 * Subscription subscription = broker.subscribe(observer);
 *
 * // Publish events
 * broker.post(new MyEvent("data"));
 */
public final class EventBroker {

    @FunctionalInterface
    private interface TypedCallable<T> {
        void callWith(T event);
    }

    /**
     * A rejection handler that defers task queuing by creating a new thread when the executor's queue is full.
     * Instead of dropping tasks or blocking the submitting thread, this policy creates a dedicated
     * "JobSubmissionPopUpThread" to handle the queuing of rejected tasks.
     * 
     * When a task is rejected (due to queue capacity being reached):
     * 1. If the executor is running:
     *    - Creates a new thread to handle the rejected task
     *    - The new thread attempts to put the task into the work queue
     *    - If the put operation is interrupted, preserves the interrupt state and
     *      throws RejectedExecutionException with the original exception as cause
     * 2. If the executor is shutdown:
     *    - Immediately throws RejectedExecutionException
     * 
     * This approach ensures:
     * - Tasks are not lost during high load
     * - The submitting thread is not blocked
     * - Task ordering is maintained through the queue
     * 
     * Note: Each rejection creates a new thread, which should be considered when
     * using this handler in scenarios with frequent queue rejections.
     */
    public static final class DeferredPopUpThreadQueueingPolicy implements RejectedExecutionHandler {

        private final BlockingQueue<Runnable> workQueue;

        DeferredPopUpThreadQueueingPolicy(final BlockingQueue<Runnable> workQueue) {
            this.workQueue = workQueue;
        }

        @Override
        public void rejectedExecution(final Runnable runnable, final ThreadPoolExecutor executor) {
            if (!executor.isShutdown()) {
                // Create new thread for the blocking put operation
                new Thread(() -> {
                    try {
                        workQueue.put(runnable);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new RejectedExecutionException("Task " + runnable + " rejected from " + executor, exception);
                    }
                }, "JobSubmissionPopUpThread").start();
            } else {
                throw new RejectedExecutionException("Task " + runnable + " rejected from " + executor);
            }
        }

    }

    /**
     * A specialized ThreadPoolExecutor that guarantees ordered execution of tasks while providing
     * configurable concurrency control. This executor ensures that tasks are processed in the
     * order they were submitted, even when using multiple worker threads.
     *
     * Key features:
     * - Maintains FIFO (First-In-First-Out) task execution order
     * - Supports custom rejection policies for queue overflow scenarios
     * - Provides configurable core and maximum thread pool sizes
     * - Uses a blocking queue for task management
     * - Handles task rejection through customizable policies
     *
     * Thread Management:
     * - Core threads are retained even when idle
     * - Additional threads are created up to maxThreads when needed
     * - Excess threads terminate after being idle for keepAliveTime
     *
     * Job Handling:
     * - Jobs are submitted to a bounded BlockingQueue
     * - When queue is full, jobs are handled by the configured RejectedExecutionHandler
     * - Default rejection policy creates pop-up threads to defer job queuing.
     *
     * Example usage:
     * OrderedThreadPoolExecutor executor = new OrderedThreadPoolExecutor(
     *     coreThreads,    // minimum number of threads
     *     maxThreads,     // maximum number of threads
     *     jobQueueCapacity,   // queue size to buffer jobs when all threads are busy
     *     queueCapacity,  // maximum queue size
     *     keepAliveTime,  // time to keep excess threads alive
     *     timeUnit        // unit for keepAliveTime
     * );
     *
     * Thread Safety: This class is thread-safe and can handle concurrent task submissions
     * from multiple threads while maintaining execution order. 
     */
    public static final class OrderedThreadPoolExecutor {

        private final Map<String, BlockingQueue<?>> bufferedEventsForInterest;  // events that need to be processed for a particular interest
        private final Map<String, AtomicBoolean> jobStatusForInterest;  // is a job handling buffered events is running for specified interest
        private final Map<String, TypedCallable<?>> callbackForInterest;  // callback to handle queued events for specified interest
        private final Map<String, Object> lastEventForInterest;  // last event handled for specified interest

        private final BlockingQueue<Runnable> scheduledJobs;
        private final ThreadPoolExecutor executor;
        private final int eventQueueCapacity;  // size of the event buffer

        public static final int EVENT_BATCH_SIZE = 250;

        OrderedThreadPoolExecutor(final int coreThreadCount, final int maxThreadCount, final int jobQueueCapacity,
                final int eventQueueCapacity, final int keepAliveTime, final TimeUnit keepAliveTimeUnit) {
            scheduledJobs = new ArrayBlockingQueue<>(jobQueueCapacity, true);
            bufferedEventsForInterest = new ConcurrentHashMap<>();
            jobStatusForInterest = new ConcurrentHashMap<>();
            callbackForInterest = new ConcurrentHashMap<>();
            lastEventForInterest = new ConcurrentHashMap<>();

            this.eventQueueCapacity = eventQueueCapacity;

            executor = new ThreadPoolExecutor(coreThreadCount, maxThreadCount, keepAliveTime, keepAliveTimeUnit,
                    scheduledJobs, Executors.defaultThreadFactory(), new DeferredPopUpThreadQueueingPolicy(scheduledJobs));
        }

        public <T> void registerCallbackForInterest(final String interestId, final TypedCallable<T> callback) {
            callbackForInterest.putIfAbsent(interestId, callback);
        }

        public boolean isCallbackRegisteredForInterest(final String interestId) {
            return callbackForInterest.containsKey(interestId);
        }

        /**
         * Submits an event for processing based on a specific interest identifier (analogous to a topic).
         * This method handles event buffering and scheduling in a thread-safe manner.
         *
         * @param <T> The type of event being submitted
         * @param interestId The identifier for the interest category/topic this event belongs to
         * @param event The event object to be processed
         *
         * The method performs the following operations:
         * 1. Creates or retrieves a blocking queue specific to the interestId
         * 2. Buffers the event in the queue, blocking if the queue is full
         * 3. Triggers job scheduling for the buffered events
         *
         * Note: This method uses a fair queuing policy to maintain FIFO ordering of events
         * within each interest category.
         *
         * @throws RuntimeException if the thread is interrupted while putting the event
         *         into the queue (wraps InterruptedException)
         */
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

        /**
         * Manages the scheduling of event processing jobs for a specific interest.
         * This method ensures that only one job is actively processing events for each interest,
         * preventing duplicate processing while maintaining event ordering.
         *
         * @param <T> The type of events being processed
         * @param interestId The identifier for the interest category being processed
         * @param eventType The class type of the events in the queue
         * @param bufferedEvents The queue containing events waiting to be processed
         *
         * Operation:
         * 1. Maintains a job status flag (AtomicBoolean) for each interest
         * 2. Uses atomic compare-and-set to ensure only one job is scheduled at a time
         * 3. If no job is running (status is false), submits a new job to the executor
         *
         * Thread Safety:
         * - Uses AtomicBoolean for thread-safe job status tracking
         * - Employs CAS (Compare-And-Set) operations to prevent race conditions
         * - Safe for concurrent access from multiple submitting threads
         *
         * Note: This method is non-blocking. If a job is already running for the given
         * interest, subsequent calls will return immediately without scheduling a new job.
         */
        private <T> void handleJobScheduling(final String interestId, final Class<T> eventType,
                final BlockingQueue<T> bufferedEvents) {
            AtomicBoolean jobStatus = jobStatusForInterest.computeIfAbsent(interestId, k -> new AtomicBoolean(false));

            if (jobStatus.compareAndSet(false, true)) {
                executor.submit(() -> processQueuedEvents(interestId, eventType, bufferedEvents, jobStatus));
            }
        }

        /**
         * Processes queued events for a specific interest in batches, ensuring duplicate events
         * are not processed and maintaining the last processed event state.
         *
         * @param <T> The type of events being processed
         * @param interestId The identifier for the interest category being processed
         * @param eventType The class type of the events in the queue
         * @param bufferedEvents The queue containing events to be processed
         * @param jobStatus Flag indicating the processing status for this interest
         *
         * Processing Logic:
         * 1. Retrieves the callback registered for this interest
         * 2. Processes events in batches of size EVENT_BATCH_SIZE
         * 3. Skips duplicate events by comparing with the last processed event
         * 4. Updates the last processed event after successful processing
         *
         * Error Handling:
         * - Individual event processing failures are caught and logged
         * - Processing continues with the next event if an error occurs
         * - Job status is always reset in the finally block
         *
         * Thread Safety:
         * - Safe for concurrent access through synchronized collections
         * - Maintains atomic job status updates
         * - Preserves event ordering within batches
         */
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
