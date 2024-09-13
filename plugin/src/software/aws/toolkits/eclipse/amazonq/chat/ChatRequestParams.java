package software.aws.toolkits.eclipse.amazonq.chat;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChatRequestParams (
    @JsonProperty("tabId") String tabId,
    @JsonProperty("prompt") ChatPrompt prompt
) {}
