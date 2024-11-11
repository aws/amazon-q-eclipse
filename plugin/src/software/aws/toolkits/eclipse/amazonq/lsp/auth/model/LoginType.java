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

    public Boolean equals(final LoginType loginType2) {
        if (loginType2 == null) {
            return false;
        }

        return this.value.equals(loginType2.getValue());
    }
}
