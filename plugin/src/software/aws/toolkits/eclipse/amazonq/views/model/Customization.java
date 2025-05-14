// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.model;

public class Customization extends Configuration {
    private final String description;
    private final Boolean isDefault;
    private final QDeveloperProfile profile;

    public Customization(final String arn, final String name, final String description, final Boolean isDefault,
            final QDeveloperProfile profile) {
        super(arn, name);
        this.description = description;
        this.isDefault = isDefault;
        this.profile = profile;
    }

    public final String getDescription() {
        return this.description;
    }

    public final Boolean getIsDefault() {
        return this.isDefault;
    }

    public final QDeveloperProfile getProfile() {
        return this.profile;
    }
}
