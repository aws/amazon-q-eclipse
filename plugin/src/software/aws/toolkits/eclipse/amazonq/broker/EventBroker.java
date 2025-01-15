// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.broker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicReference;

import software.aws.toolkits.eclipse.amazonq.subscriber.Subscriber;

public final class EventBroker {

    private static final EventBroker INSTANCE;
    private final Map<Class<?>, SubmissionPublisher<?>> publishers;

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
        SubmissionPublisher<T> publisher = (SubmissionPublisher<T>) getPublisher(event.getClass());
        publisher.submit(event);
    }

    public <T> Subscription subscribe(final Subscriber<T> subscriber) {
        SubmissionPublisher<T> publisher = getPublisher(subscriber.getSubscriptionEventClass());
        AtomicReference<Subscription> subscriptionReference = new AtomicReference<>();

        java.util.concurrent.Flow.Subscriber<T> subscriberWrapper = new java.util.concurrent.Flow.Subscriber<>() {
            private java.util.concurrent.Flow.Subscription subscription;

            @Override
            public void onSubscribe(final java.util.concurrent.Flow.Subscription subscription) {
                this.subscription = subscription;
                subscriptionReference.set(subscription);
                this.subscription.request(1);
            }

            @Override
            public void onNext(final T event) {
                subscriber.handleEvent(event);
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
                key -> new SubmissionPublisher<>());
    }

}
