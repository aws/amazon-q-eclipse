// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat;

import java.util.concurrent.CompletableFuture;

import org.eclipse.swt.browser.Browser;

import software.aws.toolkits.eclipse.amazonq.chat.models.ChatRequestParams;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatResult;
import software.aws.toolkits.eclipse.amazonq.chat.models.EncryptedChatRequestParams;
import software.aws.toolkits.eclipse.amazonq.lsp.AmazonQLspServer;
import software.aws.toolkits.eclipse.amazonq.lsp.encryption.LspEncryption;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;

public final class ChatMessage {
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

    public CompletableFuture<ChatResult> sendChatMessageWithProgress() {
        // Retrieving the chat result is expected to be a long-running process with intermittent progress notifications being sent
        // from the LSP server. The progress notifications provide a token and a partial result Object - we are utilizing a token to
        // ChatMessage mapping to acquire the associated ChatMessage so we can formulate a message for the UI.
        String partialResultToken = chatCommunicationManager.addPartialChatMessage(this);
        
        // CompletableFuture<ChatResult> chatResult = sendEncryptedChatMessage(chatRequestParams)
        // 		.thenApply(ChatResult.class::cast)
        //         .thenApply(result -> {
        //         	PluginLogger.info("Result: " + result.toString());
        //             // The mapping entry no longer needs to be maintained once the final result is retrieved.
        //             chatCommunicationManager.removePartialChatMessage(partialResultToken);
        //             return result;
        //         });

		try {
			LspEncryption lspEncryption = LspEncryption.getInstance();
			
			String encryptedMessage = lspEncryption.encode(chatRequestParams);
			
			EncryptedChatRequestParams encryptedChatRequestParams = new EncryptedChatRequestParams(
					encryptedMessage,
					chatRequestParams.getPartialResultToken()
			);
		

	       String chatResult = amazonQLspServer.sendChatPrompt(encryptedChatRequestParams).get();
	       PluginLogger.info("Chat Result: "+ chatResult);
		} catch (Exception e) {
			// TODO Auto-generated catch block
		}
       	
        return null;
    }
}
