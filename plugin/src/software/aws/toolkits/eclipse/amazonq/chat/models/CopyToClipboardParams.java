// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CopyToClipboardParams(
    @JsonProperty("tabId") String tabId,
    @JsonProperty("messageId") String messageId,
    @JsonProperty("code") String code,
    @JsonProperty("type") String type,
    @JsonProperty("referenceTrackerInformation") ReferenceTrackerInformation[] referenceTrackerInformation,
    @JsonProperty("eventId") String eventId,
    @JsonProperty("codeBlockIndex") Integer codeBlockIndex,
    @JsonProperty("totalCodeBlocks") Integer totalCodeBlocks,
    @JsonProperty("name") String name
) { };
