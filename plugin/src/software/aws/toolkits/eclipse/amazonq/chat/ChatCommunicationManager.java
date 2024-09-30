// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat;

import java.util.concurrent.CompletableFuture;
import java.util.UUID;

import org.eclipse.swt.browser.Browser;

import software.aws.toolkits.eclipse.amazonq.chat.models.ChatRequestParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatResult;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatUIInboundCommand;
import software.aws.toolkits.eclipse.amazonq.chat.models.GenericTabParams;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.util.JsonHandler;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;
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

    public CompletableFuture<ChatResult> sendMessageToChatServer(final Browser browser, final Command command, final Object params) {
        return chatMessageProvider.thenCompose(chatMessageProvider -> {
            try {
                switch (command) {
                    case CHAT_SEND_PROMPT:
                        ChatRequestParams chatRequestParams = jsonHandler.convertObject(params, ChatRequestParams.class);
                        return chatMessageProvider.sendChatPrompt(browser, chatRequestParams);
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

    public void sendMessageToChatUI(final Browser browser, final ChatUIInboundCommand command) {
        String message = jsonHandler.serialize(command);

        String script = "window.postMessage(" + message + ");";
        browser.getDisplay().asyncExec(() -> {
            browser.evaluate(script);
        });
    }

    /*
     * Gets the partial chat message using the provided token.
     */
    public ChatMessage getPartialChatMessage(final String partialResultToken) {
        return chatPartialResultMap.getValue(partialResultToken);
    }

    /*
     * Adds an entry to the partialResultToken to ChatMessage map.
     */
    public String addPartialChatMessage(final ChatMessage chatMessage) {
        String partialResultToken = UUID.randomUUID().toString();

        // Indicator for the server to send partial result notifications
        chatMessage.getChatRequestParams().setPartialResultToken(partialResultToken);

        chatPartialResultMap.setEntry(partialResultToken, chatMessage);
        return partialResultToken;
    }

    /*
     * Removes an entry from the partialResultToken to ChatMessage map.
     */
    public void removePartialChatMessage(final String partialResultToken) {
        chatPartialResultMap.removeEntry(partialResultToken);
    }
}

