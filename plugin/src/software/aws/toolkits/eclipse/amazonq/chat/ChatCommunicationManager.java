// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.ProgressParams;
import org.eclipse.swt.browser.Browser;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

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
    private final ChatPartialResultManager chatPartialResultManager;
    private Gson gson;
    

    public ChatCommunicationManager() {
        this.jsonHandler = new JsonHandler();
        this.chatMessageProvider = ChatMessageProvider.createAsync();
        this.chatPartialResultManager = ChatPartialResultManager.getInstance();
        this.gson = new Gson();
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
    
    /*
     * Handles progress notifications from the Amazon Q LSP server. Sends a chat prompt message to the webview.
     */
    public void handleProgressNotification(ProgressParams params) {
        String token;
        
        // Convert token to String
        if (params.getToken().isLeft()) {
            token = params.getToken().getLeft();
        } else {
            token = params.getToken().getRight().toString();
        }

        if (!chatPartialResultManager.shouldHandlePartialResult(token)) {
            PluginLogger.info("Not a partial result notification");
            return;
        }
        
        if (params.getValue().isLeft()) {
            PluginLogger.info("Expected object not WorkDoneProgressNotifcation");
            return;
        }
        
        Object value = params.getValue().getRight();
        
        if (!(value instanceof JsonElement)) {
            PluginLogger.info("Value is not the expected JsonElement");
            return;
        }
            
        ChatResult chatResult = gson.fromJson(((JsonElement)value), ChatResult.class);
        
        // ChatResult chatResult = jsonHandler.convertObject(paramsObject, ChatResult.class);
        
        chatPartialResultManager.handlePartialResult(this, chatResult);
    }
}
