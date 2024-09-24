// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.views;


import java.util.Objects;

import org.eclipse.lsp4j.ProgressParams;
import org.eclipse.swt.browser.Browser;

import software.aws.toolkits.eclipse.amazonq.chat.ChatCommunicationManager;
import software.aws.toolkits.eclipse.amazonq.chat.ChatMessage;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatRequestParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatUIInboundCommand;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatUIInboundCommandName;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.util.JsonHandler;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;
import software.aws.toolkits.eclipse.amazonq.util.ProgressNotficationUtils;
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
    
    
    /*
     * Handles chat progress notifications from the Amazon Q LSP server. Sends a partial chat prompt message to the webview.
     */
    public final void handlePartialResultProgressNotification(ProgressParams params) {
        String token = ProgressNotficationUtils.getToken(params);

        // Check to ensure progress notification is a partial result for chat 
        if (!chatCommunicationManager.isProcessingPartialChatMessage(token)) {
            return;
        }

        // Check to ensure Object is sent in params
        if (params.getValue().isLeft() || Objects.isNull(params.getValue().getRight())) {
            String e = "Error occurred while handling partial result notification: expected Object value";
            throw new AmazonQPluginException(e);
        }

        ChatResult chatResult = ProgressNotficationUtils.getObject(params, ChatResult.class);
        ChatMessage chatMessage = chatCommunicationManager.getPartialChatMessage(token);
        Browser browser = chatMessage.getBrowser();
        
        // Check to ensure the body has content in order to keep displaying the spinner while loading
        if (chatResult.body() == null && chatResult.body().length() == 0) {
            return;
        }
        
        Boolean isPartialResult = true;
        ChatUIInboundCommand chatUIInboundCommand = new ChatUIInboundCommand(
            ChatUIInboundCommandName.ChatPrompt.toString(),
            chatMessage.getChatRequestParams().getTabId(),
            chatResult,
            isPartialResult
        );
        
        chatCommunicationManager.sendMessageToChatUI(browser, chatUIInboundCommand);
    }
}
