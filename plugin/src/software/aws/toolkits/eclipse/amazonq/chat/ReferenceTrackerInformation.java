package software.aws.toolkits.eclipse.amazonq.chat;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReferenceTrackerInformation(
    @JsonProperty("licenseName") String licenseName,
    @JsonProperty("repository") String repository,
    @JsonProperty("url") String url,
    @JsonProperty("recommendationContentSpan") RecommendationContentSpan recommendationContentSpan,
    @JsonProperty("information") String information
){};