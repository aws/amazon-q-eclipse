// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat.models;

public enum GenericCommandName {
    Explain("Explain"),
    Refactor("Refactor"),
    Selection("Fix"),
    TriggerType("Optimize");

    private final String name;

    GenericCommandName(final String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }
}
