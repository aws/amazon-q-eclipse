// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import software.aws.toolkits.eclipse.amazonq.chat.models.ChatRequestParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatResult;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.lsp.AmazonQLspServer;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;
import software.aws.toolkits.eclipse.amazonq.views.model.Command;

public class ChatMessage {
    private final ChatRequestParams chatRequestParams;
    private final ChatPartialResultManager chatPartialResultManager;
    private final AmazonQLspServer amazonQLspServer;
    
    public ChatMessage(final AmazonQLspServer amazonQLspServer, final ChatRequestParams chatRequestParams) {
        this.amazonQLspServer = amazonQLspServer;
        this.chatRequestParams = chatRequestParams;
        this.chatPartialResultManager = ChatPartialResultManager.getInstance();
    }
    
    public ChatResult sendChatMessageWithProgress() {
        try {
            String partialResultToken = UUID.randomUUID().toString();
            chatRequestParams.setPartialResultToken(partialResultToken);

            chatPartialResultManager.setPartialResultTokenMapEntry(partialResultToken, this);

            PluginLogger.info("Sending " + Command.CHAT_SEND_PROMPT + " message to Amazon Q LSP server");
            ChatResult chatResult = amazonQLspServer.sendChatPrompt(chatRequestParams).get();
            return chatResult;
        } catch (InterruptedException | ExecutionException e) {
            PluginLogger.error("Error occurred while sending " + Command.CHAT_SEND_PROMPT + " message to Amazon Q LSP server", e);
            throw new AmazonQPluginException(e);
        }
    }
}
