// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.model;

public class Configuration {
    private final String arn;
    private final String name;

    public Configuration(final String arn, final String name) {
        this.arn = arn;
        this.name = name;
    }

    public final String getArn() {
        return this.arn;
    }

    public final String getName() {
        return this.name;
    }

}
