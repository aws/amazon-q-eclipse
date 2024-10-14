// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.util;

public interface IAutoTriggerListener {
    void onStart();

    void onShutdown();
}
