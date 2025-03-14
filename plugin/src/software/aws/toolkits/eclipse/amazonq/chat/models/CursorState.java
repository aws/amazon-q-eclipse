// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import org.eclipse.lsp4j.Range;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CursorState(
        @JsonProperty("range") Range range
) { }
