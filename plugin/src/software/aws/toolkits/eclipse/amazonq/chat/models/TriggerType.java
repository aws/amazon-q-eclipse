// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat.models;

public enum TriggerType {
    Hotkeys("hotkeys"),
    Click("click"),
    ContextMenu("contextMenu");

    private final String value;

    TriggerType(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public String toString() {
        return getValue();
    }
}