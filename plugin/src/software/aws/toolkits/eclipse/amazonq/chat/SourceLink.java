package software.aws.toolkits.eclipse.amazonq.chat;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SourceLink(
        @JsonProperty("title") String title,
        @JsonProperty("url") String url,
        @JsonProperty("body") String body
){};
