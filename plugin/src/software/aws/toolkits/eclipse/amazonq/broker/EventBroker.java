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
                    for (String key: statefulSubjectsByIdForType.get(eventType).keySet()) {
                        subjectLock.lock();
                        statefulSubjectsByIdForType.get(eventType).get(key).onNext(event);
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

    private <T> ReentrantLock getStatefulSubjectLock(final Class<T> eventType) {
        return statefulSubjectLockForType.computeIfAbsent(eventType, type -> new ReentrantLock(true));
    }

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
