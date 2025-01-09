// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.broker;

import rx.Observable;
import rx.Observer;
import rx.subjects.PublishSubject;

public final class EventBroker {

    private static final EventBroker INSTANCE;
    private final PublishSubject<Object> eventBus = PublishSubject.create();

    static {
        INSTANCE = new EventBroker();
    }

    public static EventBroker getInstance() {
        return INSTANCE;
    }

    public void postEvent(final Object event) {
        eventBus.onNext(event);
    }

    public <T> void subscribe(final Class<T> eventType, final Observer<T> observer) {
        eventBus.ofType(eventType).subscribe(observer);
    }

    public <T> Observable<T> ofObservable(final Class<T> eventType) {
        return eventBus.ofType(eventType);
    }

}
