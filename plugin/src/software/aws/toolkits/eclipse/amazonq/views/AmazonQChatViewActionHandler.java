// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.views;


import java.util.LinkedHashMap;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import software.aws.toolkits.eclipse.amazonq.chat.ChatCommunicationManager;
import software.aws.toolkits.eclipse.amazonq.chat.models.CopyToClipboardParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.InfoLinkClickParams;

import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.util.JsonHandler;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;
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
                //TODO
                break;
            case CHAT_INSERT_TO_CURSOR_POSITION:
                break;
            case CHAT_FEEDBACK:
                //TODO
                break;
            case CHAT_FOLLOW_UP_CLICK:
                //TODO
                break;
            case TELEMETRY_EVENT:
                LinkedHashMap<?, ?> paramsMap = jsonHandler.convertObject(params, LinkedHashMap.class);
                Object nameAttribute = paramsMap.get("name");
                if (nameAttribute instanceof String) {
                    String name = (String) nameAttribute;

                    // Workaround to copy to clipboard because the chat-client does not currently emit a copyToClipboard event.
                    // This intercepts a telemetry event and retrieves the code to be copied from the params. This should be replaced
                    // once the LSP server team has moved the responsibility to the server-side.
                    if (name.equals("copyToClipboard")) {
                        CopyToClipboardParams copyToClipboardParams = jsonHandler.convertObject(params, CopyToClipboardParams.class);
                        handleCopyToClipboard(copyToClipboardParams);
                    }
                }
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

    private void handleCopyToClipboard(final CopyToClipboardParams params) {
        Display display = PlatformUI.getWorkbench().getDisplay();
        String codeToCopy = params.code();

        display.asyncExec(() -> {
            Clipboard clipboard = new Clipboard(display);
            try {
                TextTransfer textTransfer = TextTransfer.getInstance();
                clipboard.setContents(new Object[]{codeToCopy}, new Transfer[]{textTransfer});
            } catch (Exception e) {
                throw new AmazonQPluginException("Failed to copy to clipboard", e);
            } finally {
                clipboard.dispose();
            }
        });
    }
}
