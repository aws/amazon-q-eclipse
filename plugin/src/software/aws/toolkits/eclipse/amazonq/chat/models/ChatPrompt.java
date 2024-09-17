// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChatPrompt(
    @JsonProperty("prompt") String prompt,
    @JsonProperty("escapedPrompt") String escapedPrompt,
    @JsonProperty("command") String command
) { }
