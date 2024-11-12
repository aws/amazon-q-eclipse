// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

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

    public String toString() {
        return getValue();
    }
}
