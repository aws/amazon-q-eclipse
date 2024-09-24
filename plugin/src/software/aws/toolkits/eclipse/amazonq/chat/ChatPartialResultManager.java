// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import software.aws.toolkits.eclipse.amazonq.chat.models.ChatResult;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;

public class ChatPartialResultManager {
    private static ChatPartialResultManager instance;
    private final Map<String, ChatMessage> partialResultTokenMap;
    
    private ChatPartialResultManager() {
        partialResultTokenMap = new ConcurrentHashMap<String, ChatMessage>();
    }
    
    public static synchronized ChatPartialResultManager getInstance() {
        if (instance == null) {
            instance = new ChatPartialResultManager();
        }
        return instance;
    }
    
    public void setPartialResultTokenMapEntry(String token, ChatMessage chatMessage) {
        partialResultTokenMap.put(token, chatMessage);
    }
    
    public void deletePartialResultTokenMapEntry(String token) {
        partialResultTokenMap.remove(token);
    }

    public Boolean shouldHandlePartialResult(String token) {
        return token != null && partialResultTokenMap.containsKey(token);
    }

    public void handlePartialResult(ChatCommunicationManager chatCommunicationManager, ChatResult chatResult){
        PluginLogger.info("Handling partial result...: " + chatResult.toString());
    }

}
