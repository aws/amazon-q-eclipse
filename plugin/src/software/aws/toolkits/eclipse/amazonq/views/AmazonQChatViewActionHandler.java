// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.views;


import org.eclipse.swt.browser.Browser;

import software.aws.toolkits.eclipse.amazonq.chat.ChatCommunicationManager;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;
import software.aws.toolkits.eclipse.amazonq.views.model.Command;
import software.aws.toolkits.eclipse.amazonq.views.model.ParsedCommand;

public class AmazonQChatViewActionHandler implements ViewActionHandler {
    ChatCommunicationManager chatCommunicationManager;
    
    public AmazonQChatViewActionHandler() {
        chatCommunicationManager = new ChatCommunicationManager();
    }
    
    @Override
    public final void handleCommand(ParsedCommand parsedCommand, final Browser browser) {
        Command command = parsedCommand.getCommand();
        Object params = parsedCommand.getParams();
        
        switch (command) {
            case CHAT_READY:
                PluginLogger.info("Chat_ready command received");
                break;
            case CHAT_TAB_ADD:
                PluginLogger.info("Chat_tab_add command received with params " + params.toString());
                chatCommunicationManager.sendMessageToChatServerAsync(command, params);
                break;
            case TELEMETRY_EVENT:
            	PluginLogger.info("Telemetry command received with params " + params.toString());
                break;
            default:
                PluginLogger.info("Unhandled command: " + parsedCommand.getCommand());
                break;
        }
    }
}
