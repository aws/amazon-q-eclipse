// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.telemetry.generator.model;

import com.fasterxml.jackson.annotation.JsonValue;

public final class MetricUnit {
    @JsonValue
    private final String type;

    private MetricUnit(final String type) {
        this.type = type;
    }

    public String getType() {
        return this.type;
    }

    public static final MetricUnit NONE = new MetricUnit("None");
    public static final MetricUnit MILLISECONDS = new MetricUnit("Milliseconds");
    public static final MetricUnit BYTES = new MetricUnit("Bytes");
    public static final MetricUnit PERCENT = new MetricUnit("Percent");
    public static final MetricUnit COUNT = new MetricUnit("Count");

    public static MetricUnit fromString(final String type) {
        return switch (type) {
            case "None" -> NONE;
            case "Milliseconds" -> MILLISECONDS;
            case "Bytes" -> BYTES;
            case "Percent" -> PERCENT;
            case "Count" -> COUNT;
            default -> throw new IllegalArgumentException("Invalid MetricUnit type: " + type);
        };
    }
}
