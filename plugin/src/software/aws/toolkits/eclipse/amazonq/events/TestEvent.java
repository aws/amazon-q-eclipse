// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.events;

public final class TestEvent {
    private final String message;

    public TestEvent(final String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

}
