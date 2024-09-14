// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.aws.toolkits.eclipse.amazonq.providers.LspProvider;
import software.aws.toolkits.eclipse.amazonq.util.ObjectMapperFactory;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;
import software.aws.toolkits.eclipse.amazonq.views.model.Command;

public class ChatCommunicationManager {
    
    private final ObjectMapper objectMapper;
    
    public ChatCommunicationManager() {
        this.objectMapper = ObjectMapperFactory.getInstance();
    }

    public void sendMessageToChatServerAsync(Command command, Object params) {
            
            String jsonParams = null;
            try {
                jsonParams = objectMapper.writeValueAsString(params);
            } catch (JsonProcessingException e) {
                PluginLogger.error("Error occurred while stringifying object for command " + command.toString());
                return;
            }
        
        
           switch(command) {
               case CHAT_TAB_ADD:
                   Optional<GenericTabParams> tabParams = toObject(jsonParams, GenericTabParams.class);
                   
                   if (tabParams == null) {
                       PluginLogger.error("Halting chat server request for " + command.toString() + ". No params provided.");
                       return;
                   }
                   
                   tabParams.ifPresent(p -> {
                       try {
                        PluginLogger.info("Sending CHAT_TAB_ADD request to LSP server");
                        LspProvider.getAmazonQServer().get().tabAdd(p).get();
                    } catch (InterruptedException | ExecutionException e) {
                        PluginLogger.error("Failed to execute chat command " + command.toString(),e);
                    }
                   });
                   break;
               default:
                   PluginLogger.error("Unhandled chat command: " + command.toString());
           }
    }
    
    private <T> Optional<T> toObject(String jsonParams, Class<T> cls) {
        try {
            T params = objectMapper.readValue(jsonParams, cls);
            return Optional.ofNullable(params);
        } catch (JsonProcessingException e) {
            PluginLogger.error("Error JSON: " + e.getMessage());
        }
        return Optional.empty();
    }
}
