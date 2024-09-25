// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat;

import java.util.concurrent.ExecutionException;

import org.eclipse.swt.browser.Browser;

import software.aws.toolkits.eclipse.amazonq.chat.models.ChatRequestParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatResult;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.lsp.AmazonQLspServer;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;
import software.aws.toolkits.eclipse.amazonq.views.model.Command;

public class ChatMessage {
    private final Browser browser;
    private final ChatRequestParams chatRequestParams;
    private final AmazonQLspServer amazonQLspServer;
    private final ChatCommunicationManager chatCommunicationManager;
    
    public ChatMessage(final AmazonQLspServer amazonQLspServer, final Browser browser, final ChatRequestParams chatRequestParams) {
        this.amazonQLspServer = amazonQLspServer;
        this.browser = browser;
        this.chatRequestParams = chatRequestParams;
        this.chatCommunicationManager = ChatCommunicationManager.getInstance();
    }
    
    public Browser getBrowser() {
        return browser;
    }
    
    public ChatRequestParams getChatRequestParams() {
        return chatRequestParams;
    }
    
    public String getPartialResultToken() {
        return chatRequestParams.getPartialResultToken();
    }
    
    public ChatResult sendChatMessageWithProgress() {
        try {
            // Retrieving the chat result is expected to be a long-running process with intermittent progress notifications being sent
            // from the LSP server. The progress notifications provide a token and a result - we are utilizing this token to
            // ChatMessage mapping to acquire the associated ChatMessage.
            String partialResultToken = chatCommunicationManager.addPartialChatMessage(this);
            
            PluginLogger.info("Sending " + Command.CHAT_SEND_PROMPT + " message to Amazon Q LSP server");
            ChatResult chatResult = amazonQLspServer.sendChatPrompt(chatRequestParams).get();
            
            // The mapping entry no longer needs to be maintained once the final result is retrieved.
            chatCommunicationManager.removePartialChatMessage(partialResultToken);
            
            return chatResult;
        } catch (InterruptedException | ExecutionException e) {
            PluginLogger.error("Error occurred while sending " + Command.CHAT_SEND_PROMPT + " message to Amazon Q LSP server", e);
            throw new AmazonQPluginException(e);
        }
    }
}
