// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChatRequestParams(
    @JsonProperty("tabId") String tabId,
    @JsonProperty("prompt") ChatPrompt prompt
) { }

