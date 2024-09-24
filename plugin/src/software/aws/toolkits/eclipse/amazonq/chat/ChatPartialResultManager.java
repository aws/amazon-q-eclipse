// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import software.aws.toolkits.eclipse.amazonq.chat.models.ChatResult;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;

public class ChatPartialResultManager {
    private static ChatPartialResultManager instance;
    private final Map<String, ChatMessage> tokenToChatMessageMap;
    
    private ChatPartialResultManager() {
        tokenToChatMessageMap = new ConcurrentHashMap<String, ChatMessage>();
    }
    
    public static synchronized ChatPartialResultManager getInstance() {
        if (instance == null) {
            instance = new ChatPartialResultManager();
        }
        return instance;
    }
    
    public void setMapEntry(String token, ChatMessage chatMessage) {
        tokenToChatMessageMap.put(token, chatMessage);
    }
    
    public void deleteMapEntry(String token) {
        tokenToChatMessageMap.remove(token);
    }
    
    public ChatMessage getValue(String token) {
        return tokenToChatMessageMap.getOrDefault(token, null);
    }
    
    public Boolean hasKey(String token) {
        return tokenToChatMessageMap.containsKey(token);
    }
}
