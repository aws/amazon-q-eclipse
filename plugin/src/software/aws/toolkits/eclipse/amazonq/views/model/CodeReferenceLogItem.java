// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CodeReferenceLogItem(
    @JsonProperty("message") String message
) { };
