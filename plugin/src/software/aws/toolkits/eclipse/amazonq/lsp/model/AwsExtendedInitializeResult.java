// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.model;

import org.eclipse.lsp4j.InitializeResult;

public class AwsExtendedInitializeResult extends InitializeResult {
    private AwsServerCapabilities awsServerCapabilities;

    public final AwsServerCapabilities getAwsServerCapabilities() {
        return awsServerCapabilities;
    }

    public final void setAwsServerCapabilities(final AwsServerCapabilities awsServerCapabilities) {
        this.awsServerCapabilities = awsServerCapabilities;
    }
}
