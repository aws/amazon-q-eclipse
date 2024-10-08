// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
package software.aws.toolkits.eclipse.amazonq.chat.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a command that is being sent to Q Chat UI.
 */
public record ChatUIInboundCommand(
    @JsonProperty("command") String command,
    @JsonProperty("tabId") String tabId,
    @JsonProperty("params") Object params,
    @JsonProperty("isPartialResult") Boolean isPartialResult
) { 
	
	public static ChatUIInboundCommand createGenericCommand(GenericCommandParams params) {
		return new ChatUIInboundCommand(
			ChatUIInboundCommandName.GenericCommand.getValue(),
			null,
			params,
			null
		);
	}
	
	public static ChatUIInboundCommand createSendToPrompCommand(SendToPromptParams params) {
		return new ChatUIInboundCommand(
			ChatUIInboundCommandName.SendToPrompt.getValue(),
			null,
			params,
			null
		);
	}
};
