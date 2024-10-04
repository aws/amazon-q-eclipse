// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.telemetry.generator.model;

import java.util.List;

public record MetricSchema(String name, String description, MetricUnit unit, List<MetadataSchema> metadata,
        boolean passive) {
    public String getNamespace() {
        return name.split("_")[0].toLowerCase();
    }
}
