// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import software.aws.toolkits.eclipse.amazonq.chat.models.ChatResult;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;

/**
 * ChatPartialResultMap is responsible for maintaining a mapping between
 * partial result tokens and the associated ChatMessage objects.
 * 
 * $/progress notifications are caught and handled in the AmazonQLspClientImpl 
 * notifyProgress method. Within a progress notification, we are provided ProgressParams
 * containing a token and a partial result object. The tokenToChatMessage map in
 * this class allows us to find the associated ChatMessage associated with the token.
 *
 * @see AmazonQLspClientImpl#notifyProgress(ProgressParams)
 */
public class ChatPartialResultMap {
    
    private final Map<String, ChatMessage> tokenToChatMessageMap;
    
    public ChatPartialResultMap() {
        tokenToChatMessageMap = new ConcurrentHashMap<String, ChatMessage>();
    }
    
    public void setEntry(String token, ChatMessage chatMessage) {
        tokenToChatMessageMap.put(token, chatMessage);
    }
    
    public void removeEntry(String token) {
        tokenToChatMessageMap.remove(token);
    }
    
    public ChatMessage getValue(String token) {
        return tokenToChatMessageMap.getOrDefault(token, null);
    }
    
    public Boolean hasKey(String token) {
        return tokenToChatMessageMap.containsKey(token);
    }
}
