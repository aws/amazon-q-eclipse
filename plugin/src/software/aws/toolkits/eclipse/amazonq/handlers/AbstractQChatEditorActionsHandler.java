// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.handlers;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.swt.widgets.Display;

import software.aws.toolkits.eclipse.amazonq.chat.ChatCommunicationManager;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatUIInboundCommand;
import software.aws.toolkits.eclipse.amazonq.chat.models.GenericCommandParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.SendToPromptParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.TriggerType;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.QEclipseEditorUtils;
import software.aws.toolkits.eclipse.amazonq.views.ViewVisibilityManager;

public abstract class AbstractQChatEditorActionsHandler extends AbstractHandler {

    @Override
    public final boolean isEnabled() {
        try {
            return Activator.getLoginService().getAuthState().isLoggedIn();
        } catch (Exception e) {
            return false;
        }
    }

    protected final void executeGenericCommand(final String genericCommandVerb) {
        try {
            openQChat();
            getSelectedText().ifPresentOrElse(
                selection -> sendGenericCommand(selection, genericCommandVerb),
                () -> Activator.getLogger().info("No text was retrieved when fetching selected text")
            );
        } catch (Exception e) {
            Activator.getLogger().error(String.format("Error executing Amazon Q %s command", genericCommandVerb), e);
        }
    }

    protected final void executeSendToPromptCommand() {
        try {
            openQChat();
            getSelectedText().ifPresentOrElse(selection -> sendToPromptCommand(selection),
                    () -> Activator.getLogger().info("No text was retrieved when fetching selected text"));
        } catch (Exception e) {
            Activator.getLogger().error("Error executing Amazon Q send to prompt command", e);
        }
    }

    private void sendToPromptCommand(final String selection) {
        SendToPromptParams params =  new SendToPromptParams(
                selection,
                TriggerType.ContextMenu.getValue()
        );

        ChatUIInboundCommand command = ChatUIInboundCommand.createSendToPromptCommand(params);
        ChatCommunicationManager.getInstance().sendMessageToChatUI(command);
    }

    private void sendGenericCommand(final String selection, final String genericCommandVerb) {
        GenericCommandParams params = new GenericCommandParams(
            null, // tabParams not utilized - flare handles sending to open tab, else new tab if loading
            selection,
            TriggerType.ContextMenu.getValue(),
            genericCommandVerb
        );
        ChatUIInboundCommand command = ChatUIInboundCommand.createGenericCommand(params);
        ChatCommunicationManager.getInstance().sendMessageToChatUI(command);
    }

    private void openQChat() {
        Display.getDefault().syncExec(() -> {
            ViewVisibilityManager.showChatView();
        });
    }

    private Optional<String> getSelectedText() {
        AtomicReference<Optional<String>> result = new AtomicReference<Optional<String>>();

        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                result.set(QEclipseEditorUtils.getSelectedText());
            }
        });

        return result.get();
    }
}
