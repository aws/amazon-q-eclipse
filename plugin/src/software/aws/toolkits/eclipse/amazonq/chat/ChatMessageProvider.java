// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat;

import java.util.concurrent.CompletableFuture;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatRequestParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatResult;
import software.aws.toolkits.eclipse.amazonq.chat.models.GenericTabParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.QuickActionParams;
import software.aws.toolkits.eclipse.amazonq.lsp.AmazonQLspServer;
import software.aws.toolkits.eclipse.amazonq.providers.LspProvider;

public final class ChatMessageProvider {

    private final AmazonQLspServer amazonQLspServer;

    public static CompletableFuture<ChatMessageProvider> createAsync() {
        return LspProvider.getAmazonQServer()
                .thenApply(ChatMessageProvider::new);
    }

    private ChatMessageProvider(final AmazonQLspServer amazonQLspServer) {
        this.amazonQLspServer = amazonQLspServer;
    }

    public CompletableFuture<ChatResult> sendChatPrompt(final ChatRequestParams chatRequestParams) {
        ChatMessage chatMessage = new ChatMessage(amazonQLspServer);
        return chatMessage.sendChatPrompt(chatRequestParams);
    }

    public CompletableFuture<ChatResult> sendQuickAction(final QuickActionParams quickActionParams) {
        ChatMessage chatMessage = new ChatMessage(amazonQLspServer);
        return chatMessage.sendQuickAction(quickActionParams);
    }

    public void sendChatReady() {
        ChatMessage chatMessage = new ChatMessage(amazonQLspServer);
        chatMessage.sendChatReady();
    }

    public void sendTabAdd(final GenericTabParams tabParams) {
        ChatMessage chatMessage = new ChatMessage(amazonQLspServer);
        chatMessage.sendTabAdd(tabParams);
    }

    public void sendTabRemove(final GenericTabParams tabParams) {
        ChatMessage chatMessage = new ChatMessage(amazonQLspServer);
        chatMessage.sendTabRemove(tabParams);
    }

    public void sendTabChange(final GenericTabParams tabParams) {
        ChatMessage chatMessage = new ChatMessage(amazonQLspServer);
        chatMessage.sendTabChange(tabParams);
    }
}
