// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.telemetry.generator.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record TelemetryDefinition(List<TelemetryMetricType> types, List<MetricDefinition> metrics) {
    @JsonCreator
    public TelemetryDefinition(
            @JsonProperty("types") final List<TelemetryMetricType> types,
            @JsonProperty("metrics") final List<MetricDefinition> metrics
    ) {
        this.types = types;
        this.metrics = metrics;
    }
}
