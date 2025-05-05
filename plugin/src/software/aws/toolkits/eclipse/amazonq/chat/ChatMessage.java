// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat;

import software.aws.toolkits.eclipse.amazonq.util.JsonHandler;
import com.fasterxml.jackson.databind.JsonNode;

public final class ChatMessage {
    private final JsonHandler jsonHandler;
    private Object data;

    public ChatMessage(final Object data) {
        this.jsonHandler = new JsonHandler();
        this.data = data;
    }

    public final boolean hasKey(final String key) {
        return jsonHandler.getValueForKey(data, key) != null;
    }

    public JsonNode getValueForKey(final String key) {
        return jsonHandler.getValueForKey(data, key);
    }

    public void addValueForKey(final String key, final Object obj) {
        data = jsonHandler.addValueForKey(data, key, obj);
    }

    public final Object getData() {
        return data;
    }

    public String getValueAsString(String key) {
        JsonNode node = jsonHandler.getValueForKey(data, key);
        return node != null ? node.asText() : null;
    }

    // possibly getAsArray(), etc. to simplify formatting

}
