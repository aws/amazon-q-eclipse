// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UpdateCredentialsPayloadData(
        @JsonProperty("data") BearerCredentials data
    ) {
}
