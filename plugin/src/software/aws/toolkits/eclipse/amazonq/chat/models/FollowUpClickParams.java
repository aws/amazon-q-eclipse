// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FollowUpClickParams(
        @JsonProperty("tabId") String tabId,
        @JsonProperty("messageId") String messageId,
        @JsonProperty("followUp") ChatItemAction followUp
) { }
