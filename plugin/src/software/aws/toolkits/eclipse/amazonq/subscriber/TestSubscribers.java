// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.subscriber;

import software.aws.toolkits.eclipse.amazonq.events.TestEvent;
import software.aws.toolkits.eclipse.amazonq.observers.EventObserver;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

public final class TestSubscribers implements EventObserver<TestEvent> {

    private int previousSequenceNumber = -1;

    public TestSubscribers() {

    }

    @Override
    public void onEvent(final TestEvent event) {
        Activator.getLogger().info(event.getMessage());

        if (event.getSequenceNumber() - previousSequenceNumber != 1) {
            Activator.getLogger().info("OUT OF ORDER: " + event.getSequenceNumber() + " " + previousSequenceNumber);
        }

        previousSequenceNumber = event.getSequenceNumber();
    }
}
