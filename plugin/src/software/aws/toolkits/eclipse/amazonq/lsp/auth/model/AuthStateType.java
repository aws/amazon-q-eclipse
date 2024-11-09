// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.auth.model;

public enum AuthStateType {
    LOGGED_IN("logged-in"),
    LOGGED_OUT("logged-out"),
    EXPIRED("expired");

    private String value;

    AuthStateType(final String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
