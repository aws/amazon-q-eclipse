// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.broker;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicReference;

import software.aws.toolkits.eclipse.amazonq.subscriber.Subscriber;

public final class EventBroker {

    private static final EventBroker INSTANCE;
    private final Map<Class<?>, SubmissionPublisher<?>> publishers;

    private final class Scheduler {

        private final ExecutorService executorService;
        private final BlockingQueue<Runnable> taskQueue;

        Scheduler() {
            executorService = Executors.newSingleThreadExecutor();
            taskQueue = new LinkedBlockingQueue<>();
            processTasks();
        }

        private void processTasks() {
            executorService.execute(() -> {
                while (true) {
                    try {
                        Runnable task = taskQueue.take();
                        task.run();
                    } catch (InterruptedException E) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        public void schedule(final Runnable task) { // Add fair semaphore
            taskQueue.offer(task);
        }

    }

    static {
        INSTANCE = new EventBroker();
    }

    private EventBroker() {
        publishers = new ConcurrentHashMap<>();
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
        publisher.submit(event);
    }

    public <T> Subscription subscribe(final Subscriber<T> subscriber) {
        SubmissionPublisher<T> publisher = getPublisher(subscriber.getEventType());
        AtomicReference<Subscription> subscriptionReference = new AtomicReference<>();

        java.util.concurrent.Flow.Subscriber<T> subscriberWrapper = new java.util.concurrent.Flow.Subscriber<>() {
            private java.util.concurrent.Flow.Subscription subscription;
            private final Scheduler scheduler = new Scheduler();

            @Override
            public void onSubscribe(final java.util.concurrent.Flow.Subscription subscription) {
                this.subscription = subscription;
                subscriptionReference.set(subscription);
                this.subscription.request(1);
            }

            @Override
            public void onNext(final T event) {
                scheduler.schedule(() -> subscriber.handleEvent(event));
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

}
