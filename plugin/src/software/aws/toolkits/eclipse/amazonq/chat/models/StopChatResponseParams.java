package software.aws.toolkits.eclipse.amazonq.chat.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StopChatResponseParams(
    @JsonProperty("tabId")
        String tabId) {
}
