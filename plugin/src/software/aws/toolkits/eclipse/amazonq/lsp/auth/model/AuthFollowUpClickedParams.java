// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.auth.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthFollowUpClickedParams(
        @JsonProperty("tabId") String tabId,
        @JsonProperty("messageId") String messageId,
        @JsonProperty("authFollowupType") String authFollowupType // {@link AuthFollowUpType}
) { };
