package software.aws.toolkits.eclipse.amazonq.chat;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FollowUp(
        @JsonProperty("text") String text,
        @JsonProperty("options") ChatItemAction[] options
){}
