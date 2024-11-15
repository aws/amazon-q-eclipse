// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.model;

public enum LanguageServerLocation {
    Remote("remote"),
    Cache("cache"),
    Override("override"),
    Fallback("fallback");

    private final String value;

    LanguageServerLocation(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public String toString() {
        return getValue();
    }
}
