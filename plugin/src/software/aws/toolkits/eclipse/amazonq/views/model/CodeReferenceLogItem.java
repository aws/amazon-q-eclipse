package software.aws.toolkits.eclipse.amazonq.views.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CodeReferenceLogItem(
    @JsonProperty("message") String message
) { };
