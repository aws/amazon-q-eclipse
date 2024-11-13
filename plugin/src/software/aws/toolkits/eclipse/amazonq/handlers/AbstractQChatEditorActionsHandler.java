// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.handlers;

import java.time.Duration;
import java.time.Instant;
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
import software.aws.toolkits.eclipse.amazonq.telemetry.EclipseTelemetryProvider;
import software.aws.toolkits.eclipse.amazonq.util.QEclipseEditorUtils;
import software.aws.toolkits.eclipse.amazonq.views.ViewVisibilityManager;
import software.aws.toolkits.telemetry.TelemetryDefinitions.Result;

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
        var start = Instant.now();
        try {
            openQChat();
            getSelectedText().ifPresentOrElse(
                selection -> {
                    sendGenericCommand(selection, genericCommandVerb);
                    emitExecuteCommand(genericCommandVerb, start, Result.SUCCEEDED, "");
                },
                () -> {
                    Activator.getLogger().info("No text was retrieved when fetching selected text");
                    emitExecuteCommand(genericCommandVerb, start, Result.FAILED, "No text was retrieved when fetching selected text");
                }
            );
        } catch (Exception e) {
            emitExecuteCommand(genericCommandVerb, start, Result.FAILED, e.getMessage());
            Activator.getLogger().error(String.format("Error executing Amazon Q %s command", genericCommandVerb), e);
        }
    }

    protected final void executeSendToPromptCommand() {
        var start = Instant.now();
        try {
            openQChat();
            getSelectedText().ifPresentOrElse(
                selection -> {
                    sendToPromptCommand(selection);
                    emitExecuteCommand("sendToPrompt", start, Result.SUCCEEDED, "");
                },
                () -> {
                    Activator.getLogger().info("No text was retrieved when fetching selected text");
                    emitExecuteCommand("sendToPrompt", start, Result.FAILED, "No text was retrieved when fetching selected text");
                }
            );
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
            ViewVisibilityManager.showChatView("shortcut");
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
    private void emitExecuteCommand(final String command, final Instant start, final Result result, final String reason) {
        double duration = Duration.between(start, Instant.now()).toMillis();
        var params = new EclipseTelemetryProvider.Params(command, duration, result, reason);
        EclipseTelemetryProvider.emitExecuteCommandMetric(params);
        return;
    }
}
