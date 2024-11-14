package software.aws.toolkits.eclipse.amazonq.views.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import software.aws.toolkits.eclipse.amazonq.chat.models.ReferenceTrackerInformation;

public record ChatCodeReference(
        @JsonProperty("references") ReferenceTrackerInformation[] references
) { }
