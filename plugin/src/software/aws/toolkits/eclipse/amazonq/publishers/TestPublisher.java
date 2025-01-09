// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.publishers;

import software.aws.toolkits.eclipse.amazonq.broker.EventBroker;
import software.aws.toolkits.eclipse.amazonq.events.TestEvent;

public final class TestPublisher {

    private final EventBroker eventBroker;

    public TestPublisher() {
        eventBroker = EventBroker.getInstance();

        for (int i = 0; i < 10; i++) {
            String message = "Test message " + i;
            eventBroker.postEvent(new TestEvent(message));
        }
    }

}
