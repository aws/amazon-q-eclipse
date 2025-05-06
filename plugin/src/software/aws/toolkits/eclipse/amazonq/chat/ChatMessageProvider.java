// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import software.aws.toolkits.eclipse.amazonq.chat.models.ButtonClickParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.ButtonClickResult;
import software.aws.toolkits.eclipse.amazonq.chat.models.EncryptedChatParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.EncryptedQuickActionParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.FeedbackParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.FileClickParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.FollowUpClickParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.GenericLinkClickParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.GenericTabParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.InsertToCursorPositionParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.PromptInputOptionChangeParams;
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

    public CompletableFuture<String> sendChatPrompt(final String tabId,
            final ChatMessage encryptedChatRequestParams) {
        var response = amazonQLspServer.sendChatPrompt(encryptedChatRequestParams.getData());
        // We assume there is only one outgoing request per tab because the input is
        // blocked when there is an outgoing request
        inflightRequestByTabId.put(tabId, response);

        return handleChatResponse(tabId, response);
    }

    public CompletableFuture<String> sendInlineChatPrompt(final ChatMessage encryptedChatRequestParams) {
        return amazonQLspServer.sendInlineChatPrompt(encryptedChatRequestParams.getData());
    }

    public CompletableFuture<String> sendQuickAction(final String tabId, final ChatMessage encryptedQuickActionParams) {
        var response = amazonQLspServer.sendQuickAction(encryptedQuickActionParams.getData());
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

    public CompletableFuture<Boolean> endChat(final ChatMessage tabParams) {
        return amazonQLspServer.endChat(tabParams.getData());
    }

    public void sendPromptInputOptionChange(final ChatMessage optionChangeParams) {
        amazonQLspServer.promptInputOptionChange(optionChangeParams.getData());
    }

    public void sendChatReady() {
        amazonQLspServer.chatReady();
    }

    public void sendTabAdd(final ChatMessage tabParams) {
        amazonQLspServer.tabAdd(tabParams.getData());
    }

    public void sendTabRemove(final ChatMessage tabParams) {
        cancelInflightRequests(tabParams.getValueAsString("tabId"));
        amazonQLspServer.tabRemove(tabParams.getData());
    }

    public void sendTabChange(final ChatMessage tabParams) {
        amazonQLspServer.tabChange(tabParams.getData());
    }

    public void sendFileClick(final ChatMessage fileClickParams) {
        amazonQLspServer.fileClick(fileClickParams.getData());
    }

    public void sendInfoLinkClick(final ChatMessage infoLinkClickParams) {
        amazonQLspServer.infoLinkClick(infoLinkClickParams.getData());
    }

    public void sendLinkClick(final ChatMessage linkClickParams) {
        amazonQLspServer.linkClick(linkClickParams.getData());
    }

    public void sendSourceLinkClick(final ChatMessage sourceLinkClickParams) {
        amazonQLspServer.sourceLinkClick(sourceLinkClickParams.getData());
    }

    public void sendInsertToCursorPositionParams(final ChatMessage insertToCursorPositionParams) {
        amazonQLspServer.insertToCursorPosition(insertToCursorPositionParams);
    }

    public void followUpClick(final ChatMessage followUpClickParams) {
        amazonQLspServer.followUpClick(followUpClickParams.getData());
    }

    public void sendTelemetryEvent(final ChatMessage params) {
        amazonQLspServer.sendTelemetryEvent(params.getData());
    }

    public void sendFeedback(final ChatMessage params) {
        amazonQLspServer.sendFeedback(params.getData());
    }

    public CompletableFuture<Object> sendListConversations(final ChatMessage params) {
        return amazonQLspServer.listConversations(params.getData());
    }

    public CompletableFuture<ButtonClickResult> sendButtonClick(final ChatMessage params) {
        return amazonQLspServer.buttonClick(params.getData());
    }

    public CompletableFuture<Object> sendConversationClick(final ChatMessage params) {
        return amazonQLspServer.conversationClick(params.getData());
    }

    public CompletableFuture<Void> sendCreatePrompt(final ChatMessage params) {
        return amazonQLspServer.createPrompt(params.getData());
    }

    public CompletableFuture<Object> sendTabBarActions(final ChatMessage params) {
        return amazonQLspServer.tabBarAction(params.getData());
    }

    public void cancelInflightRequests(final String tabId) {
        var inflightRequest  =  inflightRequestByTabId.getOrDefault(tabId, null);
        if (inflightRequest != null) {
            inflightRequest.cancel(true);
            inflightRequestByTabId.remove(tabId);
        }
    }
}
