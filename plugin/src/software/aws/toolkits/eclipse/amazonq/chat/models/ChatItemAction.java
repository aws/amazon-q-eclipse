// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChatItemAction(
        @JsonProperty("pillText") String pillText,
        @JsonProperty("prompt") String prompt,
        @JsonProperty("disabled") Boolean disabled,
        @JsonProperty("description") String description,
        @JsonProperty("type") String type
) { };

