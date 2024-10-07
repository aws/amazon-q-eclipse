// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.views;


import org.eclipse.swt.browser.Browser;

import software.aws.toolkits.eclipse.amazonq.chat.ChatCommunicationManager;

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
                //TODO
                break;
            case CHAT_FEEDBACK:
                //TODO
                break;
            case CHAT_FOLLOW_UP_CLICK:
                //TODO
                break;
            case TELEMETRY_EVENT:
                //TODO
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
}
