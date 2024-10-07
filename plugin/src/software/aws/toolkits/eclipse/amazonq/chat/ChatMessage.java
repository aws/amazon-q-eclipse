// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat;

import java.util.concurrent.CompletableFuture;

import software.aws.toolkits.eclipse.amazonq.chat.models.ChatRequestParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatResult;
import software.aws.toolkits.eclipse.amazonq.chat.models.GenericTabParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.QuickActionParams;
import software.aws.toolkits.eclipse.amazonq.lsp.AmazonQLspServer;

public final class ChatMessage {
    private final AmazonQLspServer amazonQLspServer;

    public ChatMessage(final AmazonQLspServer amazonQLspServer) {
        this.amazonQLspServer = amazonQLspServer;
    }

    public CompletableFuture<ChatResult> sendChatPrompt(final ChatRequestParams chatRequestParams) {
        return amazonQLspServer.sendChatPrompt(chatRequestParams);
    }

    public CompletableFuture<ChatResult> sendQuickAction(final QuickActionParams params) {
        return amazonQLspServer.sendQuickAction(params);
    }

    public void sendChatReady() {
        amazonQLspServer.chatReady();
    }

    public void sendTabAdd(final GenericTabParams tabParams) {
        amazonQLspServer.tabAdd(tabParams);
    }
    
    public void sendTabRemove(final GenericTabParams tabParams) {
        amazonQLspServer.tabRemove(tabParams);
    }
    
    public void sendTabChange(final GenericTabParams tabParams) {
        amazonQLspServer.tabChange(tabParams);
    }
}
