// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.views;


import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Display;

import com.fasterxml.jackson.databind.JsonNode;

import software.aws.toolkits.eclipse.amazonq.chat.ChatCommunicationManager;
import software.aws.toolkits.eclipse.amazonq.chat.models.CursorState;
import software.aws.toolkits.eclipse.amazonq.chat.models.InfoLinkClickParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.InsertToCursorPositionParams;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.util.JsonHandler;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;
import software.aws.toolkits.eclipse.amazonq.util.QEclipseEditorUtils;
import software.aws.toolkits.eclipse.amazonq.views.model.Command;
import software.aws.toolkits.eclipse.amazonq.views.model.ParsedCommand;

public class AmazonQChatViewActionHandler implements ViewActionHandler {
    private final JsonHandler jsonHandler;
    private ChatCommunicationManager chatCommunicationManager;

    public AmazonQChatViewActionHandler(final ChatCommunicationManager chatCommunicationManager) {
        this.jsonHandler = new JsonHandler();
        this.chatCommunicationManager = chatCommunicationManager;
    }

    /*
     * Handles the command message received from the webview
     */
    @Override
    public final void handleCommand(final ParsedCommand parsedCommand, final Browser browser) {
        Command command = parsedCommand.getCommand();
        Object params = parsedCommand.getParams();

        PluginLogger.info(command + " being processed by ActionHandler");

        switch (command) {
            case CHAT_SEND_PROMPT:
                chatCommunicationManager.sendMessageToChatServer(command, params);
                break;
            case CHAT_QUICK_ACTION:
                chatCommunicationManager.sendMessageToChatServer(command, params);
                break;
            case CHAT_INFO_LINK_CLICK:
            case CHAT_LINK_CLICK:
            case CHAT_SOURCE_LINK_CLICK:
                InfoLinkClickParams infoLinkClickParams = jsonHandler.convertObject(params, InfoLinkClickParams.class);
                var link = infoLinkClickParams.getLink();
                if (link == null || link.isEmpty()) {
                    throw new IllegalArgumentException("Link parameter cannot be null or empty");
                }
                handleExternalLinkClick(link);
                break;
            case CHAT_READY:
                chatCommunicationManager.sendMessageToChatServer(command, params);
                break;
            case CHAT_TAB_ADD:
                chatCommunicationManager.sendMessageToChatServer(command, params);
                break;
            case CHAT_TAB_REMOVE:
                chatCommunicationManager.sendMessageToChatServer(command, params);
                break;
            case CHAT_TAB_CHANGE:
                chatCommunicationManager.sendMessageToChatServer(command, params);
                break;
            case CHAT_END_CHAT:
                chatCommunicationManager.sendMessageToChatServer(command, params);
                break;
            case CHAT_INSERT_TO_CURSOR_POSITION:
                var insertToCursorParams = jsonHandler.convertObject(params, InsertToCursorPositionParams.class);
                var cursorState = insertAtCursor(insertToCursorParams);
                // add information about editor state and send telemetry event
                // only include files that are accessible via lsp which have absolute paths
                // When this fails, we will still send the request for amazonq_interactWithMessage telemetry
                getOpenFileUri().ifPresent(filePathUri -> {
                    insertToCursorParams.setTextDocument(new TextDocumentIdentifier(filePathUri));
                    cursorState.ifPresent(state -> insertToCursorParams.setCursorState(Arrays.asList(state)));
                });
                chatCommunicationManager.sendMessageToChatServer(Command.TELEMETRY_EVENT, insertToCursorParams);
                break;
            case CHAT_FEEDBACK:
                chatCommunicationManager.sendMessageToChatServer(command, params);
                break;
            case CHAT_FOLLOW_UP_CLICK:
                chatCommunicationManager.sendMessageToChatServer(command, params);
                break;
            case TELEMETRY_EVENT:
                // telemetry notification for insert to cursor is modified and forwarded to server in the InsertToCursorPosition handler
                if (isInsertToCursorEvent(params)) {
                    break;
                }
                chatCommunicationManager.sendMessageToChatServer(command, params);
                break;
            case AUTH_FOLLOW_UP_CLICKED:
                //TODO
                break;
            default:
                throw new AmazonQPluginException("Unhandled command in AmazonQChatViewActionHandler: " + command.toString());
        }
    }

    private void handleExternalLinkClick(final String link) {
        try {
            var result = PluginUtils.showConfirmDialog("Amazon Q", "Do you want to open the external website?\n\n" + link);
            if (result) {
                PluginUtils.openWebpage(link);
            }
        } catch (Exception ex) {
            PluginLogger.error("Failed to open url in browser", ex);
        }
    }

    /*
     *   Inserts the text present in parameters at caret position in editor
     *   and returns cursor state range from the start caret to end caret, which includes the entire inserted text range
     */
    private Optional<CursorState> insertAtCursor(final InsertToCursorPositionParams insertToCursorParams) {
        AtomicReference<Optional<Range>> range = new AtomicReference<Optional<Range>>();
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                range.set(QEclipseEditorUtils.insertAtCursor(insertToCursorParams.getCode()));
            }
        });
        return range.get().map(CursorState::new);
    }

    private boolean isInsertToCursorEvent(final Object params) {
        return Optional.ofNullable(jsonHandler.getValueForKey(params, "name"))
                .map(JsonNode::asText)
                .map("insertToCursorPosition"::equals)
                .orElse(false);
    }

    private Optional<String> getOpenFileUri() {
        AtomicReference<Optional<String>> fileUri = new AtomicReference<Optional<String>>();
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                fileUri.set(QEclipseEditorUtils.getOpenFileUri());
            }
        });
        return fileUri.get();
    }
}
