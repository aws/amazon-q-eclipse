package software.aws.toolkits.eclipse.amazonq.chat;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RecommendationContentSpan(
    @JsonProperty("start") Integer start,
    @JsonProperty("end") Integer end
){};