// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FeedbackPayload(
        @JsonProperty("messageId") String messageId,
        @JsonProperty("tabId") String tabId,
        @JsonProperty("selectedOption") String selectedOption,
        @JsonProperty("comment") String comment
    ) { }
