// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.providers.assets;

public enum WebViewAssetState {
    RESOLVED, DEPENDENCY_MISSING;

    public boolean isDependencyMissing() {
        return this == DEPENDENCY_MISSING;
    }

}
