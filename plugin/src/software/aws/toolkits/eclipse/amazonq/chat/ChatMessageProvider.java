// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import software.aws.toolkits.eclipse.amazonq.chat.models.EncryptedChatParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.EncryptedQuickActionParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.GenericTabParams;
import software.aws.toolkits.eclipse.amazonq.lsp.AmazonQLspServer;
import software.aws.toolkits.eclipse.amazonq.providers.LspProvider;

public final class ChatMessageProvider {

    private final AmazonQLspServer amazonQLspServer;
    // Map of in-flight requests per tab Ids
    // TODO ECLIPSE-349: Handle disposing resources of this class including this map
    private Map<String, CompletableFuture<String>> inflightRequestByTabId = new ConcurrentHashMap<String, CompletableFuture<String>>();

    public static CompletableFuture<ChatMessageProvider> createAsync() {
        return LspProvider.getAmazonQServer()
                .thenApply(ChatMessageProvider::new);
    }

    private ChatMessageProvider(final AmazonQLspServer amazonQLspServer) {
        this.amazonQLspServer = amazonQLspServer;
    }

    public CompletableFuture<String> sendChatPrompt(final String tabId, final EncryptedChatParams encryptedChatRequestParams) {
        ChatMessage chatMessage = new ChatMessage(amazonQLspServer);

        var response = chatMessage.sendChatPrompt(encryptedChatRequestParams);
        // We assume there is only one outgoing request per tab because the input is
        // blocked when there is an outgoing request
        inflightRequestByTabId.put(tabId, response);
        response.whenComplete((result, exception) -> {
            // stop tracking in-flight requests once response is received
            inflightRequestByTabId.remove(tabId);
        });
        return response;
    }

    public CompletableFuture<String> sendQuickAction(final EncryptedQuickActionParams encryptedQuickActionParams) {
        ChatMessage chatMessage = new ChatMessage(amazonQLspServer);
        return chatMessage.sendQuickAction(encryptedQuickActionParams);
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
