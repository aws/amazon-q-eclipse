// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.telemetry.generator.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record TelemetryMetricType(String name, String description, MetricMetadataTypes type,
        List<Object> allowedValues) {
    @JsonCreator
    public TelemetryMetricType(
        @JsonProperty("name") final String name,
        @JsonProperty("description") final String description,
        @JsonProperty("type") final MetricMetadataTypes type,
        @JsonProperty("allowedValues") final List<Object> allowedValues
    ) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.allowedValues = allowedValues;
    }
}
