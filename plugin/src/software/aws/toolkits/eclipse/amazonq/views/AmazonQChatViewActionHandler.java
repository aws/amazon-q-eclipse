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
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.PlatformUI;

import software.aws.toolkits.eclipse.amazonq.chat.models.CopyToClipboardParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.InfoLinkClickParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.InsertToCursorPositionParams;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthFollowUpClickedParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthFollowUpType;
import software.aws.toolkits.eclipse.amazonq.util.Constants;
import software.aws.toolkits.eclipse.amazonq.util.JsonHandler;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
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

        Activator.getLogger().info(command + " being processed by ActionHandler");

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
                PluginUtils.handleExternalLinkClick(link);
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
            case CHAT_COPY_TO_CLIPBOARD:
                CopyToClipboardParams copyToClipboardParams = jsonHandler.convertObject(params, CopyToClipboardParams.class);
                handleCopyToClipboard(copyToClipboardParams.code());
                break;
            case AUTH_FOLLOW_UP_CLICKED:
                AuthFollowUpClickedParams authFollowUpClickedParams = jsonHandler.convertObject(params, AuthFollowUpClickedParams.class);
                handleAuthFollowUpClicked(authFollowUpClickedParams);
                break;
            default:
                throw new AmazonQPluginException("Unhandled command in AmazonQChatViewActionHandler: " + command.toString());
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

    private void handleCopyToClipboard(final String selection) {
        Display display = PlatformUI.getWorkbench().getDisplay();

        display.asyncExec(() -> {
            Clipboard clipboard = new Clipboard(display);
            try {
                TextTransfer textTransfer = TextTransfer.getInstance();
                clipboard.setContents(new Object[]{selection}, new Transfer[]{textTransfer});
            } catch (Exception e) {
                throw new AmazonQPluginException("Failed to copy to clipboard", e);
            } finally {
                clipboard.dispose();
            }
        });
    }

    private void handleAuthFollowUpClicked(final AuthFollowUpClickedParams params) {
        String incomingType = params.authFollowupType();
        String fullAuth =  AuthFollowUpType.FULL_AUTH.getValue();
        String reAuth = AuthFollowUpType.RE_AUTH.getValue();
        String missingScopes = AuthFollowUpType.MISSING_SCOPES.getValue();
        String useSupportedAuth = AuthFollowUpType.USE_SUPPORTED_AUTH.getValue();

        try {
            if (incomingType.equals(reAuth) || incomingType.equals(missingScopes)) {
                Activator.getLoginService().reAuthenticate().get();
                return;
            }
        } catch (Exception ex) {
            PluginUtils.showErrorDialog("Amazon Q", Constants.RE_AUTHENTICATE_FAILURE_MESSAGE);
            throw new AmazonQPluginException("Failed to re-authenticate in auth follow up clicked handler", ex);
        }

        try {
            if (incomingType.equals(fullAuth) || incomingType.equals(useSupportedAuth)) {
                Display.getDefault().asyncExec(() -> {
                    AmazonQView.showView(ToolkitLoginWebview.ID);
                });
                return;
            }
        } catch (Exception ex) {
            PluginUtils.showErrorDialog("Amazon Q", Constants.AUTHENTICATE_FAILURE_MESSAGE);
            throw new AmazonQPluginException("Failed to authenticate in auth follow up clicked handler", ex);
        }

        PluginUtils.showErrorDialog("Amazon Q", Constants.AUTHENTICATE_FAILURE_MESSAGE);
        throw new AmazonQPluginException("Error occured while attempting to handle auth follow up clicked: Unknown AuthFollowUpType " + incomingType);
    }
}
