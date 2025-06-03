// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

public final class JsonHandler {
    private final ObjectMapper objectMapper;

    public JsonHandler() {
        this.objectMapper = ObjectMapperFactory.getInstance();
    }

    JsonHandler(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serialize(final Object obj) {
        String serializedObj = null;
        try {
            serializedObj = objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            Activator.getLogger().error("Error occurred while serializing object: " + obj.toString(), e);
            return null;
        }
        return serializedObj;
    }

    public <T> T deserialize(final String jsonString, final Class<T> cls) {
        try {
            T params = objectMapper.readValue(jsonString, cls);
            return params;
        } catch (JsonProcessingException e) {
            Activator.getLogger().error("Error occurred while deserializing jsonString: " + jsonString, e);
        }
        return null;
    }

    public <T> T convertObject(final Object obj, final Class<T> cls) {
        T castedObj = deserialize(serialize(obj), cls);
        return castedObj;
    }

    public JsonNode getValueForKey(final Object obj, final String key) {
        JsonNode currentNode = objectMapper.valueToTree(obj);

        String[] keyParts = key.split("\\.");
        for (String keyPart : keyParts) {
            if (currentNode == null || !currentNode.has(keyPart)) {
                return null;
            }
            currentNode = currentNode.get(keyPart);
        }

        return currentNode;
    }

    public JsonNode addValueForKey(final Object obj, final String key, final Object value) {
        ObjectNode rootNode;
        if (obj instanceof JsonNode) {
            rootNode = (ObjectNode) obj;
        } else {
            rootNode = objectMapper.valueToTree(obj);
        }

        String[] keyParts = key.split("\\.");
        ObjectNode currentNode = rootNode;

        for (int i = 0; i < keyParts.length - 1; i++) {
            String keyPart = keyParts[i];
            if (!currentNode.has(keyPart) || !currentNode.get(keyPart).isObject()) {
                currentNode.putObject(keyPart);
            }
            currentNode = (ObjectNode) currentNode.get(keyPart);
        }

        String finalKey = keyParts[keyParts.length - 1];
        if (value != null) {
            JsonNode valueNode = objectMapper.valueToTree(value);
            currentNode.set(finalKey, valueNode);
        }

        return rootNode;
    }
}
