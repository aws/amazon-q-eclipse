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
 * A thread-safe event broker that manages event publishing and subscription
 * using RxJava. This class provides a centralized mechanism for event handling
 * across the application, with support for type-safe event publishing and
 * subscription.
 */
public final class EventBroker {

    private final Subject<Object> eventBus; // the main event bus
    private final CompositeDisposable disposableSubscriptions;

    /**
     * Cache of type-specific observable streams. Each stream maintains its last
     * emitted value and is "hot" due to eagerly connecting which allows for
     * publishers to emit values event if no downstream publishers exist.
     */
    private final Map<Class<?>, Observable<?>> cachedStatefulObservablesForType;

    public EventBroker() {
        eventBus = BehaviorSubject.create().toSerialized(); // serialize for thread safety
        eventBus.subscribeOn(Schedulers.computation()); // publish on dedicated thread

        cachedStatefulObservablesForType = new ConcurrentHashMap<>();
        /*
         * This hook runs before events are published and creates a
         * ConnectableObservable before eagerly connecting to it to ensure that events
         * get published to the stream regardless of whether the stream has subscribers
         * allowing for events to be cached for late subscribers:
         */
        eventBus.doOnNext(event -> getOrCreateObservable(event.getClass())).subscribe();

        disposableSubscriptions = new CompositeDisposable();
        eventBus.doOnSubscribe(subscription -> disposableSubscriptions.add(subscription)).subscribe();
    }

    /**
     * Posts an event to the event bus. The event will be delivered to all
     * subscribers of the specific event type or cached for late subscribers.
     *
     * @param <T>   the type of the event
     * @param event the event to publish (must not be null)
     */
    public <T> void post(final T event) {
        if (event == null) {
            return;
        }
        eventBus.onNext(event);
    }

    /**
     * Gets or creates an Observable for the specified event type. The
     * ConnectedObservable maintains the last emitted value in the stream and
     * autoConnecting to the stream ensures that events are published regardless of
     * whether subscribers exist downstream. When events are emitted the latest
     * value is also cached for replay when late subscribers join.
     *
     * @param <T>       the type of events the Observable will emit
     * @param eventType the Class object representing the event type
     * @return an Observable that emits events of the specified type
     */
    @SuppressWarnings("unchecked")
    private <T> Observable<T> getOrCreateObservable(final Class<T> eventType) { // maintain stateful observables
        return (Observable<T>) cachedStatefulObservablesForType.computeIfAbsent(eventType,
                type -> eventBus.ofType(eventType).replay(1).autoConnect(-1)); // connect to stream immediately
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
        Disposable subscription = getOrCreateObservable(eventType)
                .observeOn(Schedulers.computation()) // subscribe on dedicated thread
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
        return getOrCreateObservable(eventType);
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
