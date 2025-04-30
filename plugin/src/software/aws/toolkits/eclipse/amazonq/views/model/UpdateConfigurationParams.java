// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.model;

import java.util.Map;

public final class UpdateConfigurationParams {

    private String section;
    private Map<String, Object> settings;

    public UpdateConfigurationParams(final String section, final Map<String, Object> settings) {
        this.section = section;
        this.settings = settings;
    }

    public String getSection() {
        return section;
    }

    public Map<String, Object> getSettings() {
        return settings;
    }

}
