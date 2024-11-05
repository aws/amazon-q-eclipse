// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.model;

import java.util.Map;

public record TelemetryEvent(String name, String result, Map<String, Object> data, ErrorData errorData) { }
