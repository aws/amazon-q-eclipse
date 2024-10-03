// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat;

import java.util.concurrent.CompletableFuture;

import org.eclipse.swt.browser.Browser;

import software.aws.toolkits.eclipse.amazonq.chat.models.ChatRequestParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatResult;
import software.aws.toolkits.eclipse.amazonq.chat.models.GenericTabParams;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.lsp.AmazonQLspServer;
import software.aws.toolkits.eclipse.amazonq.providers.LspProvider;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;
import software.aws.toolkits.eclipse.amazonq.views.model.Command;

public final class ChatMessageProvider {

    private final AmazonQLspServer amazonQLspServer;

    public static CompletableFuture<ChatMessageProvider> createAsync() {
        return LspProvider.getAmazonQServer()
                .thenApply(ChatMessageProvider::new);
    }

    private ChatMessageProvider(final AmazonQLspServer amazonQLspServer) {
        this.amazonQLspServer = amazonQLspServer;
    }

    public CompletableFuture<ChatResult>  sendChatPrompt(final Browser browser, final ChatRequestParams chatRequestParams) {
        ChatMessage chatMessage = new ChatMessage(amazonQLspServer, browser, chatRequestParams);
        return chatMessage.sendChatMessageWithProgress();
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
