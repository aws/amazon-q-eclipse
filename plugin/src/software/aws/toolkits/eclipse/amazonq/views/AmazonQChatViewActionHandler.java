// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.views;


import java.util.Objects;

import org.eclipse.lsp4j.ProgressParams;
import org.eclipse.swt.browser.Browser;

import software.aws.toolkits.eclipse.amazonq.chat.ChatCommunicationManager;
import software.aws.toolkits.eclipse.amazonq.chat.ChatMessage;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatRequestParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatResult;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatUIInboundCommand;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatUIInboundCommandName;
import software.aws.toolkits.eclipse.amazonq.chat.models.InfoLinkClickParams;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.util.JsonHandler;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;
import software.aws.toolkits.eclipse.amazonq.util.ProgressNotficationUtils;
import software.aws.toolkits.eclipse.amazonq.views.model.Command;
import software.aws.toolkits.eclipse.amazonq.views.model.ParsedCommand;

public class AmazonQChatViewActionHandler implements ViewActionHandler {
    private ChatCommunicationManager chatCommunicationManager;
    private final JsonHandler jsonHandler;

    public AmazonQChatViewActionHandler() {
        this.jsonHandler = new JsonHandler();
        chatCommunicationManager = ChatCommunicationManager.getInstance();
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
                chatCommunicationManager.sendMessageToChatServer(browser, command, params)
                    .thenAccept(chatResult -> {
                        ChatRequestParams chatRequestParams = jsonHandler.convertObject(params, ChatRequestParams.class);
                        ChatUIInboundCommand chatUIInboundCommand = new ChatUIInboundCommand(
                            ChatUIInboundCommandName.ChatPrompt.toString(),
                            chatRequestParams.getTabId(),
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
                var link = infoLinkClickParams.getLink();
                if (link == null || link.isEmpty()) {
                    throw new IllegalArgumentException("Link parameter cannot be null or empty");
                }
                handleExternalLinkClick(link);
                break;
            case CHAT_READY:
                chatCommunicationManager.sendMessageToChatServer(browser, command, params);
                break;
            case CHAT_TAB_ADD:
                chatCommunicationManager.sendMessageToChatServer(browser, command, params);
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

    /*
     * Handles chat progress notifications from the Amazon Q LSP server. Sends a partial chat prompt message to the webview.
     */
    public final void handlePartialResultProgressNotification(final ProgressParams params) {
        String token = ProgressNotficationUtils.getToken(params);
        ChatMessage chatMessage = chatCommunicationManager.getPartialChatMessage(token);

        if (chatMessage == null) {
            return;
        }

        // Check to ensure Object is sent in params
        if (params.getValue().isLeft() || Objects.isNull(params.getValue().getRight())) {
            throw new AmazonQPluginException("Error occurred while handling partial result notification: expected Object value");
        }

        ChatResult partialChatResult = ProgressNotficationUtils.getObject(params, ChatResult.class);
        Browser browser = chatMessage.getBrowser();

        // Check to ensure the body has content in order to keep displaying the spinner while loading
        if (partialChatResult.body() == null || partialChatResult.body().length() == 0) {
            return;
        }

        ChatUIInboundCommand chatUIInboundCommand = new ChatUIInboundCommand(
            ChatUIInboundCommandName.ChatPrompt.toString(),
            chatMessage.getChatRequestParams().getTabId(),
            partialChatResult,
            true
        );

        chatCommunicationManager.sendMessageToChatUI(browser, chatUIInboundCommand);
    }
}
