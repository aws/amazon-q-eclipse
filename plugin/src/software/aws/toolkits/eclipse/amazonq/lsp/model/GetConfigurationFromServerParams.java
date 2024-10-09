// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.model;

public class GetConfigurationFromServerParams {
    private String section;

    public final String getSection() {
        return this.section;
    }

    public final void setSection(final String section) {
        this.section = section;
    }
}
