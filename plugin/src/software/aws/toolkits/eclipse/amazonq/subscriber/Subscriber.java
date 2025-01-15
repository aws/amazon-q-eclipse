// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.subscriber;

public interface Subscriber<T> {

    Class<T> getSubscriptionEventClass();

    void handleEvent(T event);
    void handleError(Throwable error);

}
