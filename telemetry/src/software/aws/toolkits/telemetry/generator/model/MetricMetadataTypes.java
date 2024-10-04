// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.telemetry.generator.model;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonValue;
import com.squareup.javapoet.TypeName;

public final class MetricMetadataTypes {
    @JsonValue
    private final String type;

    private MetricMetadataTypes(final String type) {
        this.type = type;
    }

    public static final MetricMetadataTypes STRING = new MetricMetadataTypes("string");
    public static final MetricMetadataTypes INT = new MetricMetadataTypes("int");
    public static final MetricMetadataTypes DOUBLE = new MetricMetadataTypes("double");
    public static final MetricMetadataTypes BOOLEAN = new MetricMetadataTypes("boolean");
    public static final MetricMetadataTypes INSTANT = new MetricMetadataTypes("instant");

    public TypeName javaType() {
        return switch (this.type) {
            case "string" -> TypeName.get(String.class);
            case "int" -> TypeName.get(int.class);
            case "double" -> TypeName.get(Double.class);
            case "boolean" -> TypeName.get(boolean.class);
            case "instant" -> TypeName.get(Instant.class);
            default -> throw new IllegalArgumentException("Unsupported MetricMetadataTypes: " + this.type);
        };
    }
}
