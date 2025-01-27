// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.broker;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import software.aws.toolkits.eclipse.amazonq.broker.api.EventObserver;

public final class EventBroker {

    private final Subject<Object> eventBus = PublishSubject.create().toSerialized();

    public <T> void post(final T event) {
        if (event == null) {
            return;
        }
        eventBus.onNext(event);
    }

    public <T> Disposable subscribe(final Class<T> eventType, final EventObserver<T> observer) {
        Consumer<T> consumer = new Consumer<>() {
            @Override
            public void accept(final T event) {
                observer.onEvent(event);
            }
        };

        return eventBus.ofType(eventType)
                .observeOn(Schedulers.computation())
                .subscribe(consumer);
        }

}
