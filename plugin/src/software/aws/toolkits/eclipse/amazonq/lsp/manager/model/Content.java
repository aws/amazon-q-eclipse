// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.manager.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

public record Content(
        @JsonProperty(required = true) String filename,
        @JsonProperty(required = true) String url,
        @JsonProperty(required = true) @JsonSetter(nulls = Nulls.AS_EMPTY) List<String> hashes,
        int bytes) {
}
