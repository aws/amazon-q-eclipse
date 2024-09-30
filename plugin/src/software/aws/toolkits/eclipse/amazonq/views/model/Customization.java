// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.model;

public class Customization {
    private final String arn;
    private final String name;
    private final String description;

    public Customization(final String arn, final String name, final String description) {
        this.arn = arn;
        this.name = name;
        this.description = description;
    }

    public final String getArn() {
        return this.arn;
    }

    public final String getName() {
        return this.name;
    }

    public final String getDescription() {
        return this.description;
    }
}
