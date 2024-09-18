// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat;

import java.util.concurrent.ExecutionException;

import software.aws.toolkits.eclipse.amazonq.chat.models.ChatRequestParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatResult;
import software.aws.toolkits.eclipse.amazonq.chat.models.GenericTabParams;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.lsp.AmazonQLspServer;
import software.aws.toolkits.eclipse.amazonq.providers.LspProvider;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;
import software.aws.toolkits.eclipse.amazonq.views.model.Command;

public final class ChatMessageProvider {

    private AmazonQLspServer amazonQLspServer;

    public ChatMessageProvider() {
        try {
            amazonQLspServer = LspProvider.getAmazonQServer().get();
        } catch (InterruptedException | ExecutionException e) {
            PluginLogger.error("Error occurred while retrieving Amazon Q LSP server. Failed to instantiate ChatMessageProvider.", e);
            throw new AmazonQPluginException(e);
        }
    }

    public ChatResult sendChatPrompt(final ChatRequestParams chatRequestParams) {
        try {
            PluginLogger.info("Sending " + Command.CHAT_SEND_PROMPT + " message to Amazon Q LSP server");
            ChatResult chatResult = amazonQLspServer.sendChatPrompt(chatRequestParams).get();
            return chatResult;
        } catch (InterruptedException | ExecutionException e) {
            PluginLogger.error("Error occurred while sending " + Command.CHAT_SEND_PROMPT + " message to Amazon Q LSP server", e);
            throw new AmazonQPluginException(e);
        }
    }
    
    public void sendChatReady() {
        PluginLogger.info("Sending " + Command.CHAT_READY + " message to Amazon Q LSP server");
        amazonQLspServer.chatReady();
    }

    public void sendChatReady() {
        try {
            PluginLogger.info("Sending " + Command.CHAT_READY + " message to Amazon Q LSP server");
            amazonQLspServer.chatReady();
        } catch (Exception e) {
            PluginLogger.error("Error occurred while sending " + Command.CHAT_READY + " message to Amazon Q LSP server", e);
            throw new AmazonQPluginException(e);
        }
    }

    public void sendTabAdd(final GenericTabParams tabParams) {
        try {
            PluginLogger.info("Sending " + Command.CHAT_TAB_ADD + " message to Amazon Q LSP server");
            amazonQLspServer.tabAdd(tabParams);
        } catch (Exception e) {
            PluginLogger.error("Error occurred while sending " + Command.CHAT_TAB_ADD + " message to Amazon Q LSP server", e);
            throw new AmazonQPluginException(e);
        }
    }
}
