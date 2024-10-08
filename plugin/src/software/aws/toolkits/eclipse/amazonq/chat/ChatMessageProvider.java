// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import software.aws.toolkits.eclipse.amazonq.chat.models.ChatRequestParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatResult;
import software.aws.toolkits.eclipse.amazonq.chat.models.GenericTabParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.QuickActionParams;
import software.aws.toolkits.eclipse.amazonq.lsp.AmazonQLspServer;
import software.aws.toolkits.eclipse.amazonq.providers.LspProvider;

public final class ChatMessageProvider {

    private final AmazonQLspServer amazonQLspServer;
    // Map of in-flight requests per tab Ids
    // TODO ECLIPSE-349: Handle disposing resources of this class including this map
    private Map<String, CompletableFuture<ChatResult>> inflightRequestByTabId = new ConcurrentHashMap<String, CompletableFuture<ChatResult>>();

    public static CompletableFuture<ChatMessageProvider> createAsync() {
        return LspProvider.getAmazonQServer()
                .thenApply(ChatMessageProvider::new);
    }

    private ChatMessageProvider(final AmazonQLspServer amazonQLspServer) {
        this.amazonQLspServer = amazonQLspServer;
    }

    public CompletableFuture<ChatResult> sendChatPrompt(final ChatRequestParams chatRequestParams) {
        ChatMessage chatMessage = new ChatMessage(amazonQLspServer);

        var response = chatMessage.sendChatPrompt(chatRequestParams);
        // We assume there is only one outgoing request per tab because the input is
        // blocked when there is an outgoing request
        inflightRequestByTabId.put(chatRequestParams.getTabId(), response);
        response.whenComplete((result, exception) -> {
            // stop tracking in-flight requests once response is received
            inflightRequestByTabId.remove(chatRequestParams.getTabId());
        });
        return response;
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
        cancelInflightRequests(tabParams.tabId());
        chatMessage.sendTabRemove(tabParams);
    }

    public void sendTabChange(final GenericTabParams tabParams) {
        ChatMessage chatMessage = new ChatMessage(amazonQLspServer);
        chatMessage.sendTabChange(tabParams);
    }

    private void cancelInflightRequests(final String tabId) {
        var inflightRequest  =  inflightRequestByTabId.getOrDefault(tabId, null);
        if (inflightRequest != null) {
            inflightRequest.cancel(true);
            inflightRequestByTabId.remove(tabId);
        }
    }
}
