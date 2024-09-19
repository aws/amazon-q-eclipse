// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.views;


import org.eclipse.swt.browser.Browser;

import software.aws.toolkits.eclipse.amazonq.chat.ChatCommunicationManager;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatRequestParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatResult;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatUIInboundCommand;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatUIInboundCommandName;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.util.JsonHandler;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;
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
     * - Forwards requests to the ChatCommunicationManager which handles message sending to LSP
     * - Forwards responses to the UI using the sendMessageToUI method producing an event that is consumed by Flare chat-client
     */
    @Override
    public final void handleCommand(final ParsedCommand parsedCommand, final Browser browser) {
        Command command = parsedCommand.getCommand();
        Object params = parsedCommand.getParams();

        PluginLogger.info(command + " being processed by ActionHandler");

        switch (command) {
            case CHAT_SEND_PROMPT:
                ChatResult chatResult = chatCommunicationManager.sendMessageToChatServerAsync(command, params);

                ChatRequestParams chatRequestParams = jsonHandler.convertObject(params, ChatRequestParams.class);
                ChatUIInboundCommand chatUIInboundCommand = new ChatUIInboundCommand(
                    ChatUIInboundCommandName.ChatPrompt.toString(),
                    chatRequestParams.tabId(),
                    chatResult,
                    false
                );
                sendMessageToUI(browser, chatUIInboundCommand);
                break;
            case CHAT_READY:
                chatCommunicationManager.sendMessageToChatServerAsync(command, params);
                break;
            case CHAT_TAB_ADD:
                chatCommunicationManager.sendMessageToChatServerAsync(command, params);
                break;
            case TELEMETRY_EVENT:
                break;
            default:
                throw new AmazonQPluginException("Unhandled command in AmazonQChatViewActionHandler: " + command.toString());
        }
    }

    /**
     * Sends a message to the webview
     * 
     * See handlers in Flare chat-client: https://github.com/aws/language-servers/blob/9226fb4ed10dc54f1719b14a5b1dac1807641f79/chat-client/src/client/chat.ts#L67-L101
     */
    private void sendMessageToUI(Browser browser, ChatUIInboundCommand command) {
        String message = this.jsonHandler.serialize(command);
        String script = "window.postMessage(" + message + ");";
        browser.getDisplay().asyncExec(() -> {
            browser.evaluate(script);
        });
    }
}
