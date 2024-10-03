// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.telemetry.generator.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record MetadataDefinition(String type, Boolean required) {
    @JsonCreator
    public MetadataDefinition(
            @JsonProperty("type") final String type,
            @JsonProperty("required") final Boolean required
    ) {
        this.type = type;
        this.required = required;
    }
}
