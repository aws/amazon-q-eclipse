// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import software.aws.toolkits.eclipse.amazonq.chat.models.EncryptedChatParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.EncryptedQuickActionParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.FeedbackParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.FileClickParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.FollowUpClickParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.GenericTabParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.InsertToCursorPositionParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.PromptInputOptionChangeParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.GenericLinkClickParams;
import software.aws.toolkits.eclipse.amazonq.lsp.AmazonQLspServer;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

public final class ChatMessageProvider {

    private final AmazonQLspServer amazonQLspServer;
    // Map of in-flight requests per tab Ids
    // TODO ECLIPSE-349: Handle disposing resources of this class including this map
    private Map<String, CompletableFuture<String>> inflightRequestByTabId = new ConcurrentHashMap<String, CompletableFuture<String>>();

    public static CompletableFuture<ChatMessageProvider> createAsync() {
        return Activator.getLspProvider().getAmazonQServer()
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

        return handleChatResponse(tabId, response);
    }

    public CompletableFuture<String> sendInlineChatPrompt(final EncryptedChatParams encryptedChatRequestParams) {
        ChatMessage chatMessage = new ChatMessage(amazonQLspServer);
        return chatMessage.sendInlineChatPrompt(encryptedChatRequestParams);
    }

    public CompletableFuture<String> sendQuickAction(final String tabId, final EncryptedQuickActionParams encryptedQuickActionParams) {
        ChatMessage chatMessage = new ChatMessage(amazonQLspServer);

        var response = chatMessage.sendQuickAction(encryptedQuickActionParams);
        // We assume there is only one outgoing request per tab because the input is
        // blocked when there is an outgoing request
        inflightRequestByTabId.put(tabId, response);

        return handleChatResponse(tabId, response);
    }

    private CompletableFuture<String> handleChatResponse(final String tabId, final CompletableFuture<String> response) {
        return response.whenComplete((result, exception) -> {
            inflightRequestByTabId.remove(tabId);
        });
    }

    public CompletableFuture<Boolean> endChat(final GenericTabParams tabParams) {
        ChatMessage chatMessage = new ChatMessage(amazonQLspServer);
        return chatMessage.endChat(tabParams);
    }

    public void sendPromptInputOptionChange(final PromptInputOptionChangeParams optionChangeParams) {
        ChatMessage chatMessage = new ChatMessage(amazonQLspServer);
        chatMessage.sendPromptInputOptionChange(optionChangeParams);
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

    public void sendFileClick(final FileClickParams fileClickParams) {
        ChatMessage chatMessage = new ChatMessage(amazonQLspServer);
        chatMessage.sendFileClick(fileClickParams);
    }

    public void sendInfoLinkClick(final GenericLinkClickParams sourceLinkClickParams) {
        ChatMessage chatMessage = new ChatMessage(amazonQLspServer);
        chatMessage.sendInfoLinkClickParams(sourceLinkClickParams);
    }

    public void sendLinkClick(final GenericLinkClickParams sourceLinkClickParams) {
        ChatMessage chatMessage = new ChatMessage(amazonQLspServer);
        chatMessage.sendLinkClickParams(sourceLinkClickParams);
    }

    public void sendSourceLinkClick(final GenericLinkClickParams sourceLinkClickParams) {
        ChatMessage chatMessage = new ChatMessage(amazonQLspServer);
        chatMessage.sendSourceLinkClickParams(sourceLinkClickParams);
    }

    public void sendInsertToCursorPositionParams(final InsertToCursorPositionParams insertToCursorPositionParams) {
        ChatMessage chatMessage = new ChatMessage(amazonQLspServer);
        chatMessage.sendInsertToCursorPositionParams(insertToCursorPositionParams);
    }

    public void followUpClick(final FollowUpClickParams followUpClickParams) {
        ChatMessage chatMessage = new ChatMessage(amazonQLspServer);
        chatMessage.followUpClick(followUpClickParams);
    }

    public void sendTelemetryEvent(final Object params) {
        ChatMessage chatMessage = new ChatMessage(amazonQLspServer);
        chatMessage.sendTelemetryEvent(params);
    }

    public void sendFeedback(final FeedbackParams params) {
        ChatMessage chatMessage = new ChatMessage(amazonQLspServer);
        chatMessage.sendFeedback(params);
    }

    public CompletableFuture<Object> sendListConversations(final Object params) {
        ChatMessage chatMessage = new ChatMessage(amazonQLspServer);
        return chatMessage.sendListConversations(params);
    }

    public CompletableFuture<Object> sendConversationClick(final Object params) {
        ChatMessage chatMessage = new ChatMessage(amazonQLspServer);
        return chatMessage.sendConversationClick(params);
    }

    public CompletableFuture<Void> sendCreatePrompt(final Object params) {
        ChatMessage chatMessage = new ChatMessage(amazonQLspServer);
        return chatMessage.sendCreatePrompt(params);
    }

    private void cancelInflightRequests(final String tabId) {
        var inflightRequest  =  inflightRequestByTabId.getOrDefault(tabId, null);
        if (inflightRequest != null) {
            inflightRequest.cancel(true);
            inflightRequestByTabId.remove(tabId);
        }
    }
}
