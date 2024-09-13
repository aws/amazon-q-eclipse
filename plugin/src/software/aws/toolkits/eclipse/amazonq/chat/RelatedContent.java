package software.aws.toolkits.eclipse.amazonq.chat;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RelatedContent(
        @JsonProperty("title") String title,
        @JsonProperty("content") SourceLink[] content
){}
