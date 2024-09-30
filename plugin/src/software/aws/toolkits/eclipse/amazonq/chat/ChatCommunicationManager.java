// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat;

import java.util.concurrent.CompletableFuture;

import org.eclipse.swt.browser.Browser;

import software.aws.toolkits.eclipse.amazonq.chat.models.ChatRequestParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatResult;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatUIInboundCommand;
import software.aws.toolkits.eclipse.amazonq.chat.models.GenericTabParams;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.util.JsonHandler;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;
import software.aws.toolkits.eclipse.amazonq.views.model.Command;

public final class ChatCommunicationManager {

    private final JsonHandler jsonHandler;
    private final CompletableFuture<ChatMessageProvider> chatMessageProvider;

    public ChatCommunicationManager() {
        this.jsonHandler = new JsonHandler();
        this.chatMessageProvider = ChatMessageProvider.createAsync();
    }

    public CompletableFuture<ChatResult> sendMessageToChatServer(final Command command, final Object params) {
        return chatMessageProvider.thenCompose(chatMessageProvider -> {
            try {
                switch (command) {
                    case CHAT_SEND_PROMPT:
                        ChatRequestParams chatRequestParams = jsonHandler.convertObject(params, ChatRequestParams.class);
                        return chatMessageProvider.sendChatPrompt(chatRequestParams);
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
        String message = this.jsonHandler.serialize(command);
        String script = "window.postMessage(" + message + ");";
        browser.getDisplay().asyncExec(() -> {
            browser.evaluate(script);
        });
    }

}
