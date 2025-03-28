// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EncryptedChatParams(
        @JsonProperty("message") String message, // Message as encrypted jwt
        @JsonProperty("partialResultToken") String partialResultToken) {
}
