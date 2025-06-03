// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a command that is being sent to Q Chat UI.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatUIInboundCommand(
    @JsonProperty("command") String command,
    @JsonProperty("tabId") String tabId,
    @JsonProperty("params") Object params,
    @JsonProperty("isPartialResult") Boolean isPartialResult,
    @JsonProperty("requestId") String requestId
) {

    public static ChatUIInboundCommand createGenericCommand(final GenericCommandParams params) {
        return new ChatUIInboundCommand(
            ChatUIInboundCommandName.GenericCommand.getValue(),
            null,
            params,
            null,
            null
        );
    }

    public static ChatUIInboundCommand createSendToPromptCommand(final SendToPromptParams params) {
        return new ChatUIInboundCommand(
            ChatUIInboundCommandName.SendToPrompt.getValue(),
            null,
            params,
            null,
            null
        );
    }

    public static ChatUIInboundCommand createCommand(final String commandName, final Object params) {
        return new ChatUIInboundCommand(
            commandName,
            null,
            params,
            null,
            null
        );
    }

    public static ChatUIInboundCommand createCommand(final String commandName, final Object params, final String requestId) {
        return new ChatUIInboundCommand(
            commandName,
            null,
            params,
            null,
            requestId
        );
    }
};
