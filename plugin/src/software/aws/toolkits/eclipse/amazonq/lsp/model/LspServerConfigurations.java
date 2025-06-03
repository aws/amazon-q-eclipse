// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.model;

import java.util.List;

import software.aws.toolkits.eclipse.amazonq.views.model.Configuration;

public class LspServerConfigurations<T extends Configuration> {

    private final List<T> configurations;

    public LspServerConfigurations(final List<T> configurations) {
        this.configurations = configurations;
    }

    public final List<T> getConfigurations() {
        return this.configurations;
    }
}
