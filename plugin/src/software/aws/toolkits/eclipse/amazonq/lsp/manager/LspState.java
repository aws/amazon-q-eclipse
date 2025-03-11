// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.manager;

public enum LspState {

    ACTIVE, FAILED, PENDING;

    public boolean hasFailed() {
        return this == FAILED;
    }

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean isPending() {
        return this == PENDING;
    }

}
