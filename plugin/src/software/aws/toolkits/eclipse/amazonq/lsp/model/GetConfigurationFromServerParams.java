// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.model;

public class GetConfigurationFromServerParams {
    public enum ExpectedResponseType {
        CUSTOMIZATION, Q_DEVELOPER_PROFILE, DEFAULT
    }

    private String section;

    public GetConfigurationFromServerParams(final ExpectedResponseType responseType) {
        switch (responseType) {
        case CUSTOMIZATION:
            section = "aws.q.customizations";
            break;
        case Q_DEVELOPER_PROFILE:
            section = "aws.q.developerProfiles";
            break;
        default:
            section = "aws.q";
        }
    }

    public final String getSection() {
        return this.section;
    }

    public final void setSection(final String section) {
        this.section = section;
    }
}
