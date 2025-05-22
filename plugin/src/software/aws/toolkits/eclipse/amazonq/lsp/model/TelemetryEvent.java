// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelemetryEvent(@JsonProperty("name") String name, @JsonProperty("result") String result,
        @JsonProperty("data") Map<String, Object> data, @JsonProperty("errorData") ErrorData errorData) {
}
