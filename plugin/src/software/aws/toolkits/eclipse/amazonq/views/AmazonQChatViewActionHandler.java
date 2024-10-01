// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.views;


import org.eclipse.swt.browser.Browser;

import software.aws.toolkits.eclipse.amazonq.chat.ChatCommunicationManager;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatRequestParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatUIInboundCommand;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatUIInboundCommandName;
import software.aws.toolkits.eclipse.amazonq.chat.models.InfoLinkClickParams;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.util.JsonHandler;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;
import software.aws.toolkits.eclipse.amazonq.views.model.Command;
import software.aws.toolkits.eclipse.amazonq.views.model.ParsedCommand;

public class AmazonQChatViewActionHandler implements ViewActionHandler {
    private ChatCommunicationManager chatCommunicationManager;
    private final JsonHandler jsonHandler;

    public AmazonQChatViewActionHandler() {
        this.jsonHandler = new JsonHandler();
        chatCommunicationManager = new ChatCommunicationManager();
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
                chatCommunicationManager.sendMessageToChatServer(command, params)
                    .thenAccept(chatResult -> {
                        ChatRequestParams chatRequestParams = jsonHandler.convertObject(params, ChatRequestParams.class);
                        ChatUIInboundCommand chatUIInboundCommand = new ChatUIInboundCommand(
                            ChatUIInboundCommandName.ChatPrompt.toString(),
                            chatRequestParams.tabId(),
                            chatResult,
                            false
                        );
                        chatCommunicationManager.sendMessageToChatUI(browser, chatUIInboundCommand);
                    });
                break;
            case CHAT_INFO_LINK_CLICK:
            case CHAT_LINK_CLICK:
            case CHAT_SOURCE_LINK_CLICK:
                InfoLinkClickParams infoLinkClickParams = jsonHandler.convertObject(params, InfoLinkClickParams.class);
                handleExternalLinkClick(infoLinkClickParams.getLink());
                break;
            case CHAT_READY:
                chatCommunicationManager.sendMessageToChatServer(command, params);
                break;
            case CHAT_TAB_ADD:
                chatCommunicationManager.sendMessageToChatServer(command, params);
                break;
            case TELEMETRY_EVENT:
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
