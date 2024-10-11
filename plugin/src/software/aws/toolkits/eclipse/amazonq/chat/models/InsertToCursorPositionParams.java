// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class InsertToCursorPositionParams {
    private final String tabId;
    private final String messageId;
    private final String code;
    private final String type;
    private final ReferenceTrackerInformation[] referenceTrackerInformation;
    private final String eventId;
    private final int codeBlockIndex;
    private final int totalCodeBlocks;

    public InsertToCursorPositionParams(
        @JsonProperty("tabId") final String tabId,
        @JsonProperty("messageId") final String messageId,
        @JsonProperty("code") final String code,
        @JsonProperty("type") final String type,
        @JsonProperty("referenceTrackerInformation") final ReferenceTrackerInformation[] referenceTrackerInformation,
        @JsonProperty("eventId") final String eventId,
        @JsonProperty("codeBlockIndex") final int codeBlockIndex,
        @JsonProperty("totalCodeBlocks") final int totalCodeBlocks
    ) {
        this.tabId = tabId;
        this.messageId = messageId;
        this.code = code;
        this.type = type;
        this.referenceTrackerInformation = referenceTrackerInformation;
        this.eventId = eventId;
        this.codeBlockIndex = codeBlockIndex;
        this.totalCodeBlocks = totalCodeBlocks;
    }

    public String getCode() {
        return code;
    }
}
