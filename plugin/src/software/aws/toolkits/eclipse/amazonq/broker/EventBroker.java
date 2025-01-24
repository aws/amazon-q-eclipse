// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.broker;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import software.aws.toolkits.eclipse.amazonq.broker.api.EventObserver;
import software.aws.toolkits.eclipse.amazonq.broker.api.Subscription;

public final class EventBroker {

    private static final EventBroker INSTANCE;
    private final Subject<Object> eventBus = PublishSubject.create().toSerialized();

    static {
        INSTANCE = new EventBroker();
    }

    public static EventBroker getInstance() {
        return INSTANCE;
    }

    public <T> void post(final T event) {
        eventBus.onNext(event);
    }

    public <T> Subscription subscribe(final EventObserver<T> observer) {
        Consumer<T> consumer = new Consumer<>() {
            @Override
            public void accept(final T event) {
                observer.onEvent(event);
            }
        };
        Disposable disposable = eventBus.ofType(observer.getEventType()).distinct()
                .observeOn(Schedulers.computation())
                .subscribe(consumer);

        Subscription subscription = new Subscription() {
            @Override
            public void cancel() {
                disposable.dispose();
            }
        };
        return subscription;
    }

}
