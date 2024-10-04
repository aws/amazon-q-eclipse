// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.lsp4j.ProgressParams;

import software.aws.toolkits.eclipse.amazonq.chat.models.ChatRequestParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatResult;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatUIInboundCommand;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatUIInboundCommandName;
import software.aws.toolkits.eclipse.amazonq.chat.models.GenericTabParams;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.util.JsonHandler;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;
import software.aws.toolkits.eclipse.amazonq.util.ProgressNotficationUtils;
import software.aws.toolkits.eclipse.amazonq.views.ChatUiRequestListener;
import software.aws.toolkits.eclipse.amazonq.views.model.Command;

/**
 * ChatCommunicationManager is a central component of the Amazon Q Eclipse Plugin that
 * acts as a bridge between the plugin's UI and the LSP server. It is also responsible
 * for managing communication between the plugin and the webview used for displaying
 * chat conversations. It is implemented as a singleton to centralize control of all
 * communication in the plugin.
 */
public final class ChatCommunicationManager {
    private static ChatCommunicationManager instance;

    private final JsonHandler jsonHandler;
    private final CompletableFuture<ChatMessageProvider> chatMessageProvider;
    private final ChatPartialResultMap chatPartialResultMap;
    private ChatUiRequestListener chatUiRequestListener;

    private ChatCommunicationManager() {
        this.jsonHandler = new JsonHandler();
        this.chatMessageProvider = ChatMessageProvider.createAsync();
        this.chatPartialResultMap = new ChatPartialResultMap();
    }

    public static synchronized ChatCommunicationManager getInstance() {
        if (instance == null) {
            instance = new ChatCommunicationManager();
        }
        return instance;
    }

    public CompletableFuture<ChatResult> sendMessageToChatServer(final Command command, final Object params) {
        return chatMessageProvider.thenCompose(chatMessageProvider -> {
            try {
                switch (command) {
                    case CHAT_SEND_PROMPT:
                        ChatRequestParams chatRequestParams = jsonHandler.convertObject(params, ChatRequestParams.class);
                    return sendChatRequest(chatRequestParams.getTabId(), token -> {
                        chatRequestParams.setPartialResultToken(token);

                        return chatMessageProvider.sendChatPrompt(chatRequestParams);
                    });
                    case CHAT_READY:
                        chatMessageProvider.sendChatReady();
                        return CompletableFuture.completedFuture(null);
                    case CHAT_TAB_ADD:
                        GenericTabParams tabParams = jsonHandler.convertObject(params, GenericTabParams.class);
                        chatMessageProvider.sendTabAdd(tabParams);
                        return CompletableFuture.completedFuture(null);
                    default:
                        throw new AmazonQPluginException("Unhandled command in ChatCommunicationManager: " + command.toString());
                }
            } catch (Exception e) {
                PluginLogger.error("Error occurred in sendMessageToChatServer", e);
                return CompletableFuture.failedFuture(new AmazonQPluginException(e));
            }
        });
    }

    private CompletableFuture<ChatResult> sendChatRequest(final String tabId,
            final Function<String, CompletableFuture<ChatResult>> action) {
        // Retrieving the chat result is expected to be a long-running process with
        // intermittent progress notifications being sent
        // from the LSP server. The progress notifications provide a token and a partial
        // result Object - we are utilizing a token to
        // ChatMessage mapping to acquire the associated ChatMessage so we can formulate
        // a message for the UI.
        String partialResultToken = addPartialChatMessage(tabId);

        return action.apply(partialResultToken).thenApply(result -> {
            // The mapping entry no longer needs to be maintained once the final result is
            // retrieved.
            removePartialChatMessage(partialResultToken);
            // show chat response in Chat UI
            ChatUIInboundCommand chatUIInboundCommand = new ChatUIInboundCommand(
                    ChatUIInboundCommandName.ChatPrompt.toString(), tabId, result, false);
            sendMessageToChatUI(chatUIInboundCommand);
            return result;
        });
    }

    public void setChatUiRequestListener(final ChatUiRequestListener listener) {
        chatUiRequestListener = listener;
    }

    public void removeListener() {
        chatUiRequestListener =  null;
    }

    /*
     * Sends message to Chat UI to show in webview
     */
    public void sendMessageToChatUI(final ChatUIInboundCommand command) {
        if (chatUiRequestListener != null) {
            String message = jsonHandler.serialize(command);
            chatUiRequestListener.onSendToChatUi(message);
        }
    }

    /*
     * Handles chat progress notifications from the Amazon Q LSP server.
     * - Process partial results for Chat messages if provided token is maintained by ChatCommunicationManager
     * - Other notifications are ignored at this time.
     * - Sends a partial chat prompt message to the webview.
     */
    public void handlePartialResultProgressNotification(final ProgressParams params) {
        String token = ProgressNotficationUtils.getToken(params);
        String tabId = getPartialChatMessage(token);

        if (tabId == null || tabId.isEmpty()) {
            return;
        }

        // Check to ensure Object is sent in params
        if (params.getValue().isLeft() || Objects.isNull(params.getValue().getRight())) {
            throw new AmazonQPluginException("Error occurred while handling partial result notification: expected Object value");
        }

        ChatResult partialChatResult = ProgressNotficationUtils.getObject(params, ChatResult.class);

        // Check to ensure the body has content in order to keep displaying the spinner while loading
        if (partialChatResult.body() == null || partialChatResult.body().length() == 0) {
            return;
        }

        ChatUIInboundCommand chatUIInboundCommand = new ChatUIInboundCommand(
            ChatUIInboundCommandName.ChatPrompt.toString(),
            tabId,
            partialChatResult,
            true
        );

        sendMessageToChatUI(chatUIInboundCommand);
    }

    /*
     * Gets the partial chat message represented by the tabId using the provided token.
     */
    private String getPartialChatMessage(final String partialResultToken) {
        return chatPartialResultMap.getValue(partialResultToken);
    }

    /*
     * Adds an entry to the partialResultToken to ChatMessage's tabId map.
     */
    private String addPartialChatMessage(final String tabId) {
        String partialResultToken = UUID.randomUUID().toString();
        chatPartialResultMap.setEntry(partialResultToken, tabId);
        return partialResultToken;
    }

    /*
     * Removes an entry from the partialResultToken to ChatMessage's tabId map.
     */
    private void removePartialChatMessage(final String partialResultToken) {
        chatPartialResultMap.removeEntry(partialResultToken);
    }
}

