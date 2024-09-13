package software.aws.toolkits.eclipse.amazonq.chat;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChatPrompt (
    @JsonProperty("prompt") String prompt,
    @JsonProperty("escapedPrompt") String escapedPrompt,
    @JsonProperty("command") String command
) {}
