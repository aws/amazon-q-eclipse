// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * ChatPartialResultMap is a utility class responsible for managing the mapping between
 * partial result tokens and their corresponding ChatMessage objects in the Amazon Q plugin for Eclipse.
 *
 * The Language Server Protocol (LSP) server sends progress notifications during long-running operations,
 * such as processing chat requests. These notifications include a token that identifies the specific operation
 * and a partial result object containing the progress information.
 *
 * This class maintains a concurrent map (tokenToChatMessageMap) that associates each token with
 * its respective ChatMessage object. This mapping is crucial for correctly updating the chat UI
 * with the latest progress information as it becomes available from the LSP server.
 *
 * The progress notifications are handled by the {@link AmazonQLspClientImpl#notifyProgress(ProgressParams)}
 * method, which retrieves the corresponding ChatMessage object from the tokenToChatMessageMap using
 * the token provided in the ProgressParams. The ChatMessage can then be updated with the partial result.
 */
public final class ChatPartialResultMap {

    private final Map<String, ChatMessage> tokenToChatMessageMap;

    public ChatPartialResultMap() {
        tokenToChatMessageMap = new ConcurrentHashMap<String, ChatMessage>();
    }

    public void setEntry(final String token, final ChatMessage chatMessage) {
        tokenToChatMessageMap.put(token, chatMessage);
    }

    public void removeEntry(final String token) {
        tokenToChatMessageMap.remove(token);
    }

    public ChatMessage getValue(final String token) {
        return tokenToChatMessageMap.getOrDefault(token, null);
    }

    public Boolean hasKey(final String token) {
        return tokenToChatMessageMap.containsKey(token);
    }
}