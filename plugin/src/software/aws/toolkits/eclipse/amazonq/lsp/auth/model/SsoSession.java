// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.auth.model;

public class SsoSession extends Section {
    private SsoSessionSettings settings;

    public final SsoSessionSettings getSsoSessionSettings() {
        return settings;
    }

    public final void setSsoSessionSettings(final SsoSessionSettings settings) {
        this.settings = settings;
    }
}
