// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

public enum GenericCommandVerb {
    Explain("Explain"),
    Refactor("Refactor"),
    Fix("Fix"),
    Optimize("Optimize");

    private final String value;

    GenericCommandVerb(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return getValue();
    }
}
