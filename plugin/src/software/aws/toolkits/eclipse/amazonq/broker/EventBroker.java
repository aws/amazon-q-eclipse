// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.broker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.observables.ConnectableObservable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.Subject;
import software.aws.toolkits.eclipse.amazonq.broker.api.EventObserver;

public final class EventBroker {

    private final Subject<Object> eventBus;
    private final Map<Class<?>, Observable<?>> cachedObservables;

    public EventBroker() {
        eventBus = BehaviorSubject.create().toSerialized();

        cachedObservables = new ConcurrentHashMap<>();
        eventBus.doOnNext(event -> getOrCreateObservable(event.getClass())).subscribe();
    }

    public <T> void post(final T event) {
        if (event == null) {
            return;
        }
        eventBus.onNext(event);
    }

    @SuppressWarnings("unchecked")
    private <T> Observable<T> getOrCreateObservable(final Class<T> eventType) {
        return (Observable<T>) cachedObservables.computeIfAbsent(eventType, type -> {
            ConnectableObservable<?> observable = eventBus.ofType(type).replay(1);
            observable.connect();
            return observable;
        });
    }

    public <T> Disposable subscribe(final Class<T> eventType, final EventObserver<T> observer) {
        return getOrCreateObservable(eventType)
                .observeOn(Schedulers.computation())
                .subscribe(observer::onEvent);
    }

    public <T> Observable<T> ofObservable(final Class<T> eventType) {
        return getOrCreateObservable(eventType);
    }

}
