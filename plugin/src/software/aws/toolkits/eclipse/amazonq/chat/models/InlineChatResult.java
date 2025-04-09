// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

// Mynah-ui will not render the partial result if null values are included. Must ignore nulls values.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record InlineChatResult(
    @JsonProperty("requestId") String requestId,
    @JsonProperty("body") String body,
    @JsonProperty("messageId") String messageId,
    @JsonProperty("canBeVoted") Boolean canBeVoted,
    @JsonProperty("relatedContent") RelatedContent relatedContent,
    @JsonProperty("followUp") FollowUp followUp,
    @JsonProperty("codeReference") ReferenceTrackerInformation[] codeReference
) { };
