package software.aws.toolkits.eclipse.amazonq.chat.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CopyToClipboardParams(
        @JsonProperty("tabId") String tabId,
        @JsonProperty("messageId") String messageId,
        @JsonProperty("code") String code,
        @JsonProperty("type") String type, // "selection" | "block"
        @JsonProperty("referenceTrackerInformation") ReferenceTrackerInformation[] referenceTrackerInformation,
        @JsonProperty("eventId") String eventId,
        @JsonProperty("codeBlockIndex") Integer codeBlockIndex,
        @JsonProperty("totalCodeBlocks") Integer totalCodeBlocks
) { };