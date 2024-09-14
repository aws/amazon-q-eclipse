package software.aws.toolkits.eclipse.amazonq.chat;

import java.util.concurrent.ExecutionException;

import software.aws.toolkits.eclipse.amazonq.lsp.AmazonQLspServer;
import software.aws.toolkits.eclipse.amazonq.providers.LspProvider;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;
import software.aws.toolkits.eclipse.amazonq.views.model.Command;

public class ChatMessageProvider {
    
    private AmazonQLspServer amazonQLspServer;
    
    public ChatMessageProvider() {
        try {
            amazonQLspServer = LspProvider.getAmazonQServer().get();
        } catch (InterruptedException | ExecutionException e) {
            PluginLogger.error("Error occurred while retrieving Amazon Q LSP server. Failed to instantiate ChatMessageProvider.");
        }
    }
    
    public void sendTabAdd(GenericTabParams tabParams) {
        if (tabParams == null) {
            PluginLogger.error("Chat server request halted for " + Command.CHAT_TAB_ADD + ". No params provided.");
            return;
        }
        
         try {
             PluginLogger.info("Sending " + Command.CHAT_TAB_ADD + " message to Amazon Q LSP server");
             amazonQLspServer.tabAdd(tabParams).get();
         } catch (InterruptedException | ExecutionException e) {
             PluginLogger.error("Error occurred while sending ");
         }
    }
}
