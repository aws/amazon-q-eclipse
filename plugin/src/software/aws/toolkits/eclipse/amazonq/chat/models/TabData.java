// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import software.aws.toolkits.eclipse.amazonq.chat.ChatMessage;

public record TabData(@JsonProperty("placeholderText") String placeholderText,
        @JsonProperty("messages") List<ChatMessage> messages) {

}
