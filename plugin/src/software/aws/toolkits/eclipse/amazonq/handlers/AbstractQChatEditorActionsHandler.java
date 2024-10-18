// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.handlers;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.swt.widgets.Display;

import software.aws.toolkits.eclipse.amazonq.chat.ChatCommunicationManager;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatUIInboundCommand;
import software.aws.toolkits.eclipse.amazonq.chat.models.GenericCommandParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.SendToPromptParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.TriggerType;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.util.DefaultLoginService;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
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

        String selection = getSelectedTextOrCurrentLine();

        if (selection == null || selection.isEmpty()) {
            Activator.getLogger().info("No text was retrieved when fetching selected text or current line");
            return;
        }

        try {
            GenericCommandParams params =  new GenericCommandParams(
                  null, // tabParams not utilized - flare handles sending to open tab, else new tab if loading
                  selection,
                  TriggerType.ContextMenu.getValue(),
                  genericCommandVerb
            );

            ChatUIInboundCommand command = ChatUIInboundCommand.createGenericCommand(params);

            ChatCommunicationManager.getInstance().sendMessageToChatUI(command);
        } catch (Exception e) {
            throw new AmazonQPluginException("Error executing generic command", e);
        }
    }

    protected final void executeSendToPromptCommand() {
        // TODO: Open the Q Chat window if it is closed https://sim.amazon.com/issues/ECLIPSE-361

        String selection = getSelectedTextOrCurrentLine();

        if (selection == null || selection.isEmpty()) {
            Activator.getLogger().info("No text was retrieved when fetching selected text or current line");
            return;
        }

        try {
            SendToPromptParams params =  new SendToPromptParams(
                    selection,
                    TriggerType.ContextMenu.getValue()
            );

            ChatUIInboundCommand command = ChatUIInboundCommand.createSendToPromptCommand(params);

            ChatCommunicationManager.getInstance().sendMessageToChatUI(command);
        } catch (Exception e) {
            throw new AmazonQPluginException("Error executing sent to prompt command", e);
        }
    }

    private String getSelectedTextOrCurrentLine() {
        final String[] result = new String[1];

        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                result[0] = QEclipseEditorUtils.getSelectedTextOrCurrentLine();
            }
        });

        return result[0];
    }
}
