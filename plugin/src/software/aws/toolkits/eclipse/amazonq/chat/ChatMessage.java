// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat;

import software.aws.toolkits.eclipse.amazonq.lsp.AmazonQLspServer;
import software.aws.toolkits.eclipse.amazonq.providers.LspProvider;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;

public class ChatMessage {
    private AmazonQLspServer amazonQLspServer;
    
    public ChatMessage() {
        try {
            this.amazonQLspServer = LspProvider.getAmazonQServer().get();
        } catch (Exception e) {
            PluginLogger.error("Unable to retrieve AmazonQLspServer during ChatMessage insantiation.");
            return;
        }
    }
    
    public ChatResult sendChatRequestAsync(ChatRequestParams chatRequestParams) {
        return sendEncryptedRequestWithProgress(chatRequestParams);
    }
    
    public ChatResult sendEncryptedRequestWithProgress(ChatRequestParams chatRequestParams) {
        // TODO: Implement encryption
        
    }
}
