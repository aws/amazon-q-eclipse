// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.telemetry.generator.model;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record MetricDefinition(String name, String description, MetricUnit unit, List<MetadataDefinition> metadata,
        boolean passive) {
    @JsonCreator
    public MetricDefinition(
        @JsonProperty("name") final String name,
        @JsonProperty("description") final String description,
        @JsonProperty("unit") final MetricUnit unit,
        @JsonProperty("metadata") final List<MetadataDefinition> metadata,
        @JsonProperty("passive") final boolean passive
    ) {
        this.name = name;
        this.description = description;
        this.unit = unit;
        this.metadata = metadata != null ? metadata : Collections.emptyList();
        this.passive = passive;
    }
}
