// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.model;

public final class UpdateConfigurationParams {

    private final String section;
    private final QDeveloperProfileSettings settings;

    public UpdateConfigurationParams(final String section, final QDeveloperProfileSettings settings) {
        this.section = section;
        this.settings = settings;
    }

    public String getSection() {
        return section;
    }

    public QDeveloperProfileSettings getQDeveloperSettings() {
        return settings;
    }

}
