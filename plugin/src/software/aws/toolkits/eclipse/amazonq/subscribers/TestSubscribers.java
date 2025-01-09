// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.subscribers;

import rx.Observer;
import software.aws.toolkits.eclipse.amazonq.broker.EventBroker;
import software.aws.toolkits.eclipse.amazonq.events.TestEvent;

public final class TestSubscribers {

    public TestSubscribers() {
        fromObserver();
        fromObservable();
    }

    public void fromObserver() {
        Observer<TestEvent> observer = new Observer<>() {
            @Override
            public void onNext(final TestEvent event) {
                System.out.println(event.getMessage());
            }

            @Override
            public void onError(final Throwable error) {
                System.out.println(error.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("Completed");
            }
        };

        // Direct subscription
        EventBroker.getInstance().subscribe(TestEvent.class, observer);

        // Subscribe using observable
        EventBroker.getInstance().ofObservable(TestEvent.class).subscribe(observer);
    }

    public void fromObservable() {
        EventBroker.getInstance().ofObservable(TestEvent.class).subscribe(
                event -> System.out.println(event.getMessage()),
                throwable -> System.out.println(throwable.getMessage()), () -> System.out.println("Complete"));
    }

}
