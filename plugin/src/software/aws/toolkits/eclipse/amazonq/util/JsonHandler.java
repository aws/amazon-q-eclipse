// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonInclude;

public final class JsonHandler {
    private final ObjectMapper objectMapper;

    public JsonHandler() {
        this.objectMapper = ObjectMapperFactory.getInstance();
    }

    public String serialize(final Object obj) {
        String serializedObj = null;
        try {
            serializedObj = objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            PluginLogger.error("Error occurred while serializing object: " + obj.toString(), e);
            return null;
        }
        return serializedObj;
    }
    
    public String serialize(final Object obj, Boolean ignoreNull) {
        String serializedObj = null;
        try {
            ObjectMapper mapper = objectMapper.copy();
            if (ignoreNull) {
                mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            }
            serializedObj = mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            PluginLogger.error("Error occurred while serializing object: " + obj.toString(), e);
            return null;
        }
        return serializedObj;
    }

    public <T> T deserialize(final String jsonString, final Class<T> cls) {
        try {
            T params = objectMapper.readValue(jsonString, cls);
            return params;
        } catch (JsonProcessingException e) {
            PluginLogger.error("Error occurred while deserializing jsonString: " + jsonString, e);
        }
        return null;
    }

    public <T> T convertObject(final Object obj, final Class<T> cls) {
        T castedObj = deserialize(serialize(obj), cls);
        return castedObj;
    }
}
