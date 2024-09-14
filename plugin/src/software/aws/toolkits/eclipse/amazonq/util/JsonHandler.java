package software.aws.toolkits.eclipse.amazonq.util;

import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonHandler {
    private final ObjectMapper objectMapper;
    
    public JsonHandler() {
        this.objectMapper = ObjectMapperFactory.getInstance();
    }
    
    public String serialize(Object obj) {
        String serializedObj = null;
        try {
            serializedObj = objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            PluginLogger.error("Error occurred while serializing object: " + obj.toString(), e);
            return null;
        }
        return serializedObj;
    }
    
    public <T> Optional<T> deserialize(String jsonString, Class<T> cls) {
        try {
            T params = objectMapper.readValue(jsonString, cls);
            return Optional.ofNullable(params);
        } catch (JsonProcessingException e) {
            PluginLogger.error("Error occurred while deserializing jsonString: " + jsonString ,e);
        }
        return Optional.empty();
    }
}
