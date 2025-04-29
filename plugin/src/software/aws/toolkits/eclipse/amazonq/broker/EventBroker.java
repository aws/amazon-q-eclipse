// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.broker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.Subject;
import software.aws.toolkits.eclipse.amazonq.broker.api.EventObserver;

/**
 * A thread-safe event broker that implements the publish-subscribe pattern
 * using RxJava.
 *
 * This broker manages event distribution using BehaviorSubjects, which cache
 * the most recent event for each event type. It provides type-safe event
 * publishing and subscription, with automatic resource management for
 * subscriptions. Events are published and consumed on dedicated threads so
 * operations are non-blocking.
 */
public final class EventBroker {

    /** Maps event types to their corresponding subjects for event distribution. */
    private final Map<Class<?>, Subject<Object>> subjectsForType;

    /** Tracks all subscriptions for proper cleanup. */
    private final CompositeDisposable disposableSubscriptions;

    public EventBroker() {
        subjectsForType = new ConcurrentHashMap<>();
        disposableSubscriptions = new CompositeDisposable();
    }

    /**
     * Posts an event of the specified type to all subscribers and caches it for
     * late-subscribers.
     *
     * @param <T>       The type of the event
     * @param eventType The class object representing the event type
     * @param event     The event to publish
     */
    public <T> void post(final Class<T> eventType, final T event) {
        if (event == null) {
            return;
        }
        getOrCreateSubject(eventType).onNext(event);
    }

    /**
     * Gets or creates a Subject for the specified event type. Creates a new
     * serialized BehaviorSubject if none exists.
     *
     * @param <T>       The type of events the subject will handle
     * @param eventType The class object representing the event type
     * @return A Subject that handles events of the specified type
     */
    private <T> Subject<Object> getOrCreateSubject(final Class<T> eventType) {
        return subjectsForType.computeIfAbsent(eventType, k -> {
            Subject<Object> subject = BehaviorSubject.create().toSerialized();
            subject.subscribeOn(Schedulers.computation());
            return subject;
        });
    }

    /**
     * Subscribes an observer to events of a specific type. The observer will
     * receive events on a computation thread by default. The subscription is
     * automatically tracked for disposal management.
     *
     * @param <T>       the type of events to observe
     * @param eventType the Class object representing the event type
     * @param observer  the observer that will handle emitted events
     * @return a Disposable that can be used to unsubscribe from the events
     */
    public <T> Disposable subscribe(final Class<T> eventType, final EventObserver<T> observer) {
        Disposable subscription = ofObservable(eventType)
                .observeOn(Schedulers.computation()) // subscribe on dedicated thread
                .subscribe(observer::onEvent);
        disposableSubscriptions.add(subscription); // track subscription for dispose call
        return subscription;
    }

    /**
     * Subscribes an observer to events of a specific type, replaying all cached events.
     * The observer will receive all cached events followed by new events on a computation thread.
     *
     * @param <T>       the type of events to observe
     * @param eventType the Class object representing the event type
     * @param observer  the observer that will handle emitted events
     * @return a Disposable that can be used to unsubscribe from the events
     */
    public <T> Disposable subscribeWithReplay(final Class<T> eventType, final EventObserver<T> observer) {
        Disposable subscription = ofObservable(eventType)
                .replay()
                .autoConnect()
                .observeOn(Schedulers.computation())
                .subscribe(observer::onEvent);
        disposableSubscriptions.add(subscription);
        return subscription;
    }

    /**
     * Returns an Observable for the specified event type. This Observable can be
     * used to create custom subscription chains with additional operators.
     *
     * @param <T>       the type of events the Observable will emit
     * @param eventType the Class object representing the event type
     * @return an Observable that emits events of the specified type
     */
    public <T> Observable<T> ofObservable(final Class<T> eventType) {
        return getOrCreateSubject(eventType).ofType(eventType);
    }

    /**
     * Disposes of all subscriptions managed by this broker by clearing the disposable subscriptions collection.
     * This method should be called when the broker is no longer needed to prevent memory leaks.
     * After disposal, any existing subscriptions will be terminated and new events will not be delivered
     * to their observers.
     *
     * Note: This only disposes of the subscriptions, not the underlying Observables.
     * The EventBroker can be reused after disposal by creating new subscriptions.
     */
    public void dispose() {
        disposableSubscriptions.clear();
    }

}
