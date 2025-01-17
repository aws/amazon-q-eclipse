// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.publishers;

import software.aws.toolkits.eclipse.amazonq.broker.EventBroker;
import software.aws.toolkits.eclipse.amazonq.events.TestEvent;

public final class TestPublisher {

    public TestPublisher() {
        Thread publisherThread = new Thread(() -> {
            try {
                Thread.sleep(5000);
                EventBroker eventBroker = EventBroker.getInstance();

                for (int i = 0; i < 100000; i++) {
                    eventBroker.post(new TestEvent("Test Event " + i, i));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "TestPublisher-Thread");

        publisherThread.start();
    }

}
