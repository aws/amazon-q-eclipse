// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.handlers;

import org.eclipse.core.commands.AbstractHandler;

import software.aws.toolkits.eclipse.amazonq.chat.ChatCommunicationManager;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatUIInboundCommand;
import software.aws.toolkits.eclipse.amazonq.chat.models.GenericCommandParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.SendToPromptParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.TriggerType;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.util.AuthUtils;
import software.aws.toolkits.eclipse.amazonq.util.EditorUtils;

public abstract class QContextMenuHandler extends AbstractHandler {
	
	@Override
    public final boolean isEnabled() {
        try {
			return AuthUtils.isLoggedIn().get();
		} catch (Exception e) {
			throw new AmazonQPluginException("Error retrieving login status for QContextMenuHandler", e);
		}
    }
	
    protected void executeGenericCommand(String genericCommandVerb) {
        EditorUtils.getSelectedText()
            .thenApplyAsync(selection -> new GenericCommandParams(
                null,
                selection,
                TriggerType.ContextMenu.getValue(),
                genericCommandVerb
            ))
            .thenApplyAsync(ChatUIInboundCommand::createGenericCommand)
            .thenAcceptAsync(command -> 
                ChatCommunicationManager.getInstance().sendMessageToChatUI(command)
            )
            .exceptionally(e -> {
                throw new AmazonQPluginException("Error executing generic command", e);
            });
    }

    protected void executeSendToPromptCommand() {
        EditorUtils.getSelectedText()
            .thenApplyAsync(selection -> new SendToPromptParams(
                selection,
                TriggerType.ContextMenu.getValue()
            ))
            .thenApplyAsync(ChatUIInboundCommand::createSendToPromptCommand)
            .thenAcceptAsync(command -> 
                ChatCommunicationManager.getInstance().sendMessageToChatUI(command)
            )
            .exceptionally(e -> {
                throw new AmazonQPluginException("Error executing send to prompt command", e);
            });
    }
}
