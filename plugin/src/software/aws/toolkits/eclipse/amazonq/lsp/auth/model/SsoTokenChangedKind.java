// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.auth.model;

public enum SsoTokenChangedKind {
    EXPIRED("Expired"),
    REFRESHED("Refreshed");

    private final String value;

    SsoTokenChangedKind(final String value) {
        this.value = value;
    }

    public static SsoTokenChangedKind fromValue(final String value) {
        for (SsoTokenChangedKind kind : values()) {
            if (kind.getValue().equalsIgnoreCase(value)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("No enum constant " + SsoTokenChangedKind.class.getSimpleName() + " with value " + value);
    }

    public String getValue() {
        return this.value;
    }
}
