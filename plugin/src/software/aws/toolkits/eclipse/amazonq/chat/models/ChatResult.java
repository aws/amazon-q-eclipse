// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChatResult(
    @JsonProperty("body") String body,
    @JsonProperty("messageId") String messageId,
    @JsonProperty("canBeVoted") Boolean canBeVoted,
    @JsonProperty("relatedContent") RelatedContent relatedContent,
    @JsonProperty("followUp") FollowUp followUp,
    @JsonProperty("codeReference") ReferenceTrackerInformation[] codeReference
) { };
