// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.auth.model;

public enum LoginType {
    BUILDER_ID("AwsBuilderId"),
    IAM_IDENTITY_CENTER("IamIdentityCenter"),
    NONE("None");

    private final String value;

    LoginType(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
