// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.model;

import java.util.List;

import software.aws.toolkits.eclipse.amazonq.views.model.Customization;

public class LspServerConfigurations {

    private final List<Customization> customizations;

    public LspServerConfigurations(final List<Customization> customizations) {
        this.customizations = customizations;
    }

    public final List<Customization> getCustomizations() {
        return this.customizations;
    }
}
