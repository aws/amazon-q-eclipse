// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.broker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;
import software.aws.toolkits.eclipse.amazonq.broker.api.EventObserver;
import software.aws.toolkits.eclipse.amazonq.broker.api.MissedReplayEventObserver;

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

    /**
     * Maps event types to their corresponding state-less subjects for event distribution.
     * Subjects do not support replay mechanism.
     */
    private final Map<Class<?>, Subject<Object>> statelessSubjectsForType;

    /**
     * Maps event types to their corresponding state-ful subjects for event
     * distribution. Subjects will replay messages from the last time when there
     * were no subscribers.
     */
    private final Map<Class<?>, Map<String, Subject<Object>>> statefulSubjectsByIdForType;

    /**
     * Maps event types to their corresponding locks for thread-safe access to
     * stateful subjects. Each event type has its own fair lock to synchronize
     * access to its stateful subjects.
     */
    private final Map<Class<?>, ReentrantLock> statefulSubjectLockForType;

    /** Tracks all subscriptions for proper cleanup. */
    private final CompositeDisposable disposableSubscriptions;

    public EventBroker() {
        statelessSubjectsForType = new ConcurrentHashMap<>();
        statefulSubjectsByIdForType = new ConcurrentHashMap<>();
        statefulSubjectLockForType = new ConcurrentHashMap<>();
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
        getOrCreateStatelessSubject(eventType).onNext(event);
    }

    /**
     * Gets or creates a Subject for the specified event type. Creates a new
     * serialized BehaviorSubject if none exists.
     *
     * @param <T>       The type of events the subject will handle
     * @param eventType The class object representing the event type
     * @return A Subject that handles events of the specified type
     */
    private <T> Subject<Object> getOrCreateStatelessSubject(final Class<T> eventType) {
        return statelessSubjectsForType.computeIfAbsent(eventType, k -> {
            Subject<Object> subject = BehaviorSubject.create().toSerialized();
            Disposable subscription = subject.doOnNext(event -> {
                if (statefulSubjectsByIdForType.containsKey(eventType)) {
                    ReentrantLock subjectLock = getStatefulSubjectLock(eventType);
                    for (String subscriberId : statefulSubjectsByIdForType.get(eventType).keySet()) {
                        subjectLock.lock();
                        statefulSubjectsByIdForType.get(eventType).get(subscriberId).onNext(event);
                        subjectLock.unlock();
                    }
                }
            }).subscribe();
            disposableSubscriptions.add(subscription);
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
     * Subscribes an observer to events of a specific type with replay
     * functionality. This method is designed for observers that need to receive
     * events that were missed while they were not subscribed. The observer will
     * receive events on a computation thread by default.
     *
     * When disposed, this subscription will: 1. Create a new ReplaySubject for
     * future events 2. Complete the old subject to free resources 3. Dispose of the
     * underlying subscription
     *
     * @param <T>       the type of events to observe
     * @param eventType the Class object representing the event type
     * @param observer  the observer that will handle emitted events and contains
     *                  component ID
     * @return a Disposable that can be used to unsubscribe from the events and
     *         cleanup resources
     */
    public <T> Disposable subscribe(final Class<T> eventType, final MissedReplayEventObserver<T> observer) {
        Observable<T> observable = ofMissedObservable(eventType, observer.getSubscribingComponentId());
        Disposable subscription = observable
                .observeOn(Schedulers.computation()) // subscribe on dedicated thread
                .subscribe(observer::onEvent);
        disposableSubscriptions.add(subscription); // track subscription for dispose call
        return new Disposable() {
            private volatile boolean disposed = false;

            @Override
            public void dispose() {
                if (!disposed) {
                    disposed = true;
                    ReentrantLock subjectLock = getStatefulSubjectLock(eventType);
                    try {
                        subjectLock.lock();
                        Subject<Object> newSubject = ReplaySubject.create().toSerialized();
                        Subject<Object> oldSubject = getStatefulSubject(eventType,
                                observer.getSubscribingComponentId());
                        statefulSubjectsByIdForType.get(eventType).put(observer.getSubscribingComponentId(),
                                newSubject);
                        oldSubject.onComplete();
                        subscription.dispose();
                    } finally {
                        subjectLock.unlock();
                    }
                }
            }

            @Override
            public boolean isDisposed() {
                return disposed;
            }
        };
    }

    /**
     * Gets or creates a ReentrantLock for the specified event type. The lock is
     * used to synchronize access to stateful subjects for that event type. Creates
     * a new fair ReentrantLock if none exists for the event type.
     *
     * @param <T>       The type parameter for the event class
     * @param eventType The class object representing the event type
     * @return A ReentrantLock instance for the specified event type
     */
    private <T> ReentrantLock getStatefulSubjectLock(final Class<T> eventType) {
        return statefulSubjectLockForType.computeIfAbsent(eventType, type -> new ReentrantLock(true));
    }

    /**
     * Gets or creates a stateful Subject for a specific event type and subscriber
     * ID.
     *
     * @param <T>          The type parameter for the event class
     * @param eventType    The class object representing the event type
     * @param subscriberId The unique identifier for the subscriber
     * @return A Subject instance that handles events for the specified type and
     *         subscriber. If a Subject already exists for this subscriber, returns
     *         the existing one. Otherwise creates and returns a new serialized
     *         ReplaySubject.
     */
    private <T> Subject<Object> getStatefulSubject(final Class<T> eventType, final String subscriberId) {
        Map<String, Subject<Object>> subjectById = statefulSubjectsByIdForType.computeIfAbsent(eventType,
                type -> new ConcurrentHashMap<>());
        if (subjectById.containsKey(subscriberId)) {
            return subjectById.get(subscriberId);
        }
        return ReplaySubject.create().toSerialized();
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
        return getOrCreateStatelessSubject(eventType).ofType(eventType);
    }

    /**
     * Returns an Observable for the specified event type and subscriber ID that
     * includes missed events. This Observable is backed by a stateful subject that
     * maintains event history for replay.
     *
     * @param <T>          the type of events the Observable will emit
     * @param eventType    the Class object representing the event type
     * @param subscriberId the unique identifier for the subscriber
     * @return an Observable that emits both missed and new events of the specified
     *         type for the given subscriber
     */
    public <T> Observable<T> ofMissedObservable(final Class<T> eventType, final String subscriberId) {
        Map<String, Subject<Object>> subjectById = statefulSubjectsByIdForType.computeIfAbsent(eventType,
                type -> new ConcurrentHashMap<>());
        Observable<T> observable = subjectById
                .computeIfAbsent(subscriberId, subjectId -> {
                    return getStatefulSubject(eventType, subscriberId);
                }).ofType(eventType);
        return observable;
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
