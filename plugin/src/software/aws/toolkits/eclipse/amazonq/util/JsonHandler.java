// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        var paramsNode = objectMapper.valueToTree(obj);
        if (paramsNode.has(key)) {
            return paramsNode.get(key);
        }
        return null;
    }
}
