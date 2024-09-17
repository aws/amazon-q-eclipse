// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.views;


import org.eclipse.swt.browser.Browser;

import software.aws.toolkits.eclipse.amazonq.chat.ChatCommunicationManager;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;
import software.aws.toolkits.eclipse.amazonq.views.model.Command;
import software.aws.toolkits.eclipse.amazonq.views.model.ParsedCommand;

public class AmazonQChatViewActionHandler implements ViewActionHandler {
    private ChatCommunicationManager chatCommunicationManager;

    public AmazonQChatViewActionHandler() {
        chatCommunicationManager = new ChatCommunicationManager();
    }

    @Override
    public final void handleCommand(final ParsedCommand parsedCommand, final Browser browser) {
        Command command = parsedCommand.getCommand();
        Object params = parsedCommand.getParams();

        PluginLogger.info(command + " being processed by ActionHandler");

        switch (command) {
            case CHAT_READY:
                break;
            case CHAT_TAB_ADD:
                chatCommunicationManager.sendMessageToChatServerAsync(command, params);
                break;
            case TELEMETRY_EVENT:
                break;
            default:
                PluginLogger.info("Unhandled command: " + parsedCommand.getCommand());
                break;
        }
    }
}
