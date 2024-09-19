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

    @Override
    public final void handleCommand(final ParsedCommand parsedCommand, final Browser browser) {
        Command command = parsedCommand.getCommand();
        Object params = parsedCommand.getParams();
        String jsonParams = jsonHandler.serialize(params);

        PluginLogger.info(command + " being processed by ActionHandler");

        switch (command) {
            case CHAT_SEND_PROMPT:
                ChatResult chatResult = chatCommunicationManager.sendMessageToChatServer(command, params);
                ChatRequestParams chatRequestParams = jsonHandler.deserialize(jsonParams, ChatRequestParams.class);
                ChatUIInboundCommand chatUIInboundCommand = new ChatUIInboundCommand(
                    ChatUIInboundCommandName.ChatPrompt.toString(),
                    chatRequestParams.tabId(),
                    chatResult,
                    false
                );
                sendMessageToUI(browser, chatUIInboundCommand);
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
    
    private void sendMessageToUI(Browser browser, ChatUIInboundCommand command) {
        String message = this.jsonHandler.serialize(command);
        PluginLogger.info("Sending message to UI: " + message);

        String script = "window.postMessage(" + message + ");";
        PluginLogger.info("Script: " + script);

        browser.getDisplay().asyncExec(() -> {
            browser.evaluate(script);
        });
    }
}
