// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.events;

public final class TestEvent {
    private final String message;
    private final int sequenceNumber;

    public TestEvent(final String message, final int sequenceNumber) {
        this.message = message;
        this.sequenceNumber = sequenceNumber;
    }

    public String getMessage() {
        return message;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

}
