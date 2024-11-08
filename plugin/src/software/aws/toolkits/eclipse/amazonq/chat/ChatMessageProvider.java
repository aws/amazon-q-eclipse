// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.lsp4j.jsonrpc.messages.Either;

import software.aws.toolkits.eclipse.amazonq.chat.models.EncryptedChatParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.EncryptedQuickActionParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.ErrorParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.FeedbackParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.FollowUpClickParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.GenericTabParams;
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

    public CompletableFuture<Either<String, ErrorParams>> sendChatPrompt(final String tabId, final EncryptedChatParams encryptedChatRequestParams) {
        ChatMessage chatMessage = new ChatMessage(amazonQLspServer);

        var response = chatMessage.sendChatPrompt(encryptedChatRequestParams);
        // We assume there is only one outgoing request per tab because the input is
        // blocked when there is an outgoing request
        inflightRequestByTabId.put(tabId, response);

        return handleResponseChatResponse(tabId, response);
    }

    public CompletableFuture<Either<String, ErrorParams>> sendQuickAction(final String tabId, final EncryptedQuickActionParams encryptedQuickActionParams) {
        ChatMessage chatMessage = new ChatMessage(amazonQLspServer);

        var response = chatMessage.sendQuickAction(encryptedQuickActionParams);
        // We assume there is only one outgoing request per tab because the input is
        // blocked when there is an outgoing request
        inflightRequestByTabId.put(tabId, response);

        return handleResponseChatResponse(tabId, response);
    }

    private CompletableFuture<Either<String, ErrorParams>> handleResponseChatResponse(final String tabId, final CompletableFuture<String> response) {
        return response.handle((result, exception) -> {
            inflightRequestByTabId.remove(tabId);

            if (exception != null) {
                Activator.getLogger().error("An error occurred while processing chat request.", exception);
                String errorTitle = "An error occurred while processing your request.";
                String errorMessage = String.format("Details: %s", exception.getMessage());
                ErrorParams errorParams = new ErrorParams(tabId, null, errorMessage, errorTitle);
                return Either.forRight(errorParams);
            } else {
                return Either.forLeft(result);
            }
        });
    }

    public CompletableFuture<Boolean> endChat(final GenericTabParams tabParams) {
        ChatMessage chatMessage = new ChatMessage(amazonQLspServer);
        return chatMessage.endChat(tabParams);
    }

    public void sendChatReady() {
        ChatMessage chatMessage = new ChatMessage(amazonQLspServer);
        chatMessage.sendChatReady();
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

    private void cancelInflightRequests(final String tabId) {
        var inflightRequest  =  inflightRequestByTabId.getOrDefault(tabId, null);
        if (inflightRequest != null) {
            inflightRequest.cancel(true);
            inflightRequestByTabId.remove(tabId);
        }
    }
}
