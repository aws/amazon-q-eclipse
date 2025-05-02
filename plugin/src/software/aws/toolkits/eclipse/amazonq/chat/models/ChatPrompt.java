// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChatPrompt(
    @JsonProperty("prompt") String prompt,
    @JsonProperty("escapedPrompt") String escapedPrompt,
    @JsonProperty("command") String command,
    @JsonProperty("context") List<Object> context
) { }
