// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.handlers;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.commands.AbstractHandler;

import software.aws.toolkits.eclipse.amazonq.chat.ChatCommunicationManager;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatUIInboundCommand;
import software.aws.toolkits.eclipse.amazonq.chat.models.GenericCommandParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.SendToPromptParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.TriggerType;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.util.DefaultLoginService;
import software.aws.toolkits.eclipse.amazonq.util.QEclipseEditorUtils;

public abstract class AbstractQChatEditorActionsHandler extends AbstractHandler {

    @Override
    public final boolean isEnabled() {
        try {
            return DefaultLoginService.getInstance().getLoginDetails()
                    .thenApply(loginDetails -> loginDetails.getIsLoggedIn())
                    .get(5, TimeUnit.SECONDS);
        }  catch (TimeoutException e) {
            throw new AmazonQPluginException("Timeout retrieving login status", e);
        } catch (Exception e) {
            throw new AmazonQPluginException("Error retrieving login status for QContextMenuHandler", e);
        }
    }

    protected final void executeGenericCommand(final String genericCommandVerb) {
    	// TODO: Open the Q Chat window if it is closed https://sim.amazon.com/issues/ECLIPSE-361
    	
        QEclipseEditorUtils.getSelectedTextOrCurrentLine()
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

    protected final void executeSendToPromptCommand() {
    	// TODO: Open the Q Chat window if it is closed https://sim.amazon.com/issues/ECLIPSE-361   	

        QEclipseEditorUtils.getSelectedTextOrCurrentLine()
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
