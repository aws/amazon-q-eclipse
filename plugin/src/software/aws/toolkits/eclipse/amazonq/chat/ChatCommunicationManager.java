// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat;

import software.aws.toolkits.eclipse.amazonq.chat.models.ChatRequestParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatResult;
import software.aws.toolkits.eclipse.amazonq.chat.models.GenericTabParams;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.util.JsonHandler;
import software.aws.toolkits.eclipse.amazonq.views.model.Command;

public final class ChatCommunicationManager {

    private final JsonHandler jsonHandler;
    private final ChatMessageProvider chatMessageProvider;

    public ChatCommunicationManager() {
        this.jsonHandler = new JsonHandler();
        this.chatMessageProvider = new ChatMessageProvider();
    }

    public ChatResult sendMessageToChatServerAsync(final Command command, final Object params) {

           String jsonParams = jsonHandler.serialize(params);

           switch (command) {
               case CHAT_SEND_PROMPT:
                   ChatRequestParams chatRequestParams = jsonHandler.deserialize(jsonParams, ChatRequestParams.class);
                   ChatResult result = chatMessageProvider.sendChatPrompt(chatRequestParams);
                   return result;
               case CHAT_READY:
                   chatMessageProvider.sendChatReady();
                   return null;
               case CHAT_TAB_ADD:
                   GenericTabParams tabParams = jsonHandler.deserialize(jsonParams, GenericTabParams.class);
                   chatMessageProvider.sendTabAdd(tabParams);
                   return null;
               default:
                   throw new AmazonQPluginException("Unhandled command in ChatCommunicationManager: " + command.toString());
           }
    }
}
