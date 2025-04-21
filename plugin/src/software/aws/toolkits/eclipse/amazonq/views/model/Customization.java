// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.model;

public class Customization extends Configuration {
    private final String description;

    public Customization(final String arn, final String name, final String description) {
        super(arn, name);
        this.description = description;
    }

    public final String getDescription() {
        return this.description;
    }
}
