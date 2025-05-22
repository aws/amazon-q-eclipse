// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.handlers;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.swt.widgets.Display;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import software.aws.toolkits.eclipse.amazonq.broker.events.AmazonQLspState;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatUIInboundCommand;
import software.aws.toolkits.eclipse.amazonq.chat.models.GenericCommandParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.SendToPromptParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.TriggerType;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.telemetry.ToolkitTelemetryProvider;
import software.aws.toolkits.eclipse.amazonq.telemetry.metadata.ExceptionMetadata;
import software.aws.toolkits.eclipse.amazonq.util.QEclipseEditorUtils;
import software.aws.toolkits.eclipse.amazonq.views.ViewVisibilityManager;
import software.aws.toolkits.telemetry.TelemetryDefinitions.Result;

public abstract class AbstractQChatEditorActionsHandler extends AbstractHandler {

    private final Observable<AuthState> authStateObservable;
    private final Observable<AmazonQLspState> lspStateObservable;

    private record PluginState(AuthState authState, AmazonQLspState lspState) {
    }

    public AbstractQChatEditorActionsHandler() {
        authStateObservable = Activator.getEventBroker().ofObservable(AuthState.class);
        lspStateObservable = Activator.getEventBroker().ofObservable(AmazonQLspState.class);
    }

    public final PluginState getState() {
        return Observable.combineLatest(authStateObservable, lspStateObservable, PluginState::new)
                .observeOn(Schedulers.computation()).blockingFirst();
    }

    @Override
    public final boolean isEnabled() {
        try {
            PluginState pluginState = getState();
            return pluginState.authState.isLoggedIn() && !(pluginState.lspState == AmazonQLspState.FAILED);
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
                    emitExecuteCommand(genericCommandVerb, start, Result.SUCCEEDED, null);
                },
                () -> {
                    Activator.getLogger().info("No text was retrieved when fetching selected text");
                    emitExecuteCommand(genericCommandVerb, start, Result.FAILED, "No text was retrieved when fetching selected text");
                }
            );
        } catch (Exception e) {
            emitExecuteCommand(genericCommandVerb, start, Result.FAILED, ExceptionMetadata.scrubException("Error executing command", e));
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
                    emitExecuteCommand("sendToPrompt", start, Result.SUCCEEDED, null);
                },
                () -> {
                    Activator.getLogger().info("No text was retrieved when fetching selected text");
                    emitExecuteCommand("sendToPrompt", start, Result.FAILED, "No text was retrieved when fetching selected text");
                }
            );
        } catch (Exception e) {
            Activator.getLogger().error("Error executing Amazon Q send to prompt command", e);
            emitExecuteCommand("sendToPrompt", start, Result.FAILED, ExceptionMetadata.scrubException("Error executing command", e));
        }
    }

    private void sendToPromptCommand(final String selection) {
        SendToPromptParams params =  new SendToPromptParams(
                selection,
                TriggerType.ContextMenu.getValue()
        );

        ChatUIInboundCommand command = ChatUIInboundCommand.createSendToPromptCommand(params);
        Activator.getEventBroker().post(ChatUIInboundCommand.class, command);
    }

    private void sendGenericCommand(final String selection, final String genericCommandVerb) {
        GenericCommandParams params = new GenericCommandParams(
            null, // tabParams not utilized - flare handles sending to open tab, else new tab if loading
            selection,
            TriggerType.ContextMenu.getValue(),
            genericCommandVerb
        );
        ChatUIInboundCommand command = ChatUIInboundCommand.createGenericCommand(params);
        Activator.getEventBroker().post(ChatUIInboundCommand.class, command);
    }

    private void openQChat() {
        Display.getDefault().syncExec(() -> {
            ViewVisibilityManager.showDefaultView("shortcut");
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
        var params = new ToolkitTelemetryProvider.ExecuteParams(command, duration, result, reason);
        ToolkitTelemetryProvider.emitExecuteCommandMetric(params);
        return;
    }
}
