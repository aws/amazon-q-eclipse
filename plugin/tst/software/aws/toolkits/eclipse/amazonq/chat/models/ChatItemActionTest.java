// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ChatItemActionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String pillText = "Click me";
    private final String prompt = "Test prompt";
    private final Boolean disabled = false;
    private final String description = "Test description";
    private final String type = "button";

    @Test
    void testRecordConstructionAndGetters() {
        ChatItemAction action = new ChatItemAction(pillText, prompt, disabled, description, type);

        assertEquals(pillText, action.pillText());
        assertEquals(prompt, action.prompt());
        assertEquals(disabled, action.disabled());
        assertEquals(description, action.description());
        assertEquals(type, action.type());
    }

    @Test
    void testJsonSerialization() throws Exception {
        ChatItemAction action = new ChatItemAction(pillText, prompt, disabled, description, type);

        String serializedObject = objectMapper.writeValueAsString(action);

        assertEquals(serializedObject,
                "{\"pillText\":\"Click me\",\"prompt\":\"Test prompt\",\"disabled\":false,\"description\":\"Test description\",\"type\":\"button\"}");
    }

    @Test
    void testDeserialization() throws Exception {
        String json = "{\"pillText\":\"Click me\",\"prompt\":\"Test prompt\",\"disabled\":false,\"description\":\"Test description\",\"type\":\"button\"}";

        ChatItemAction chatItemAction = assertDoesNotThrow(() -> objectMapper.readValue(json, ChatItemAction.class));

        assertEquals(pillText, chatItemAction.pillText());
        assertEquals(prompt, chatItemAction.prompt());
        assertEquals(disabled, chatItemAction.disabled());
        assertEquals(type, chatItemAction.type());
    }

    @Test
    void testDeserializationException() throws Exception {
        String json = "incorrectly formatted json";

        assertThrows(JsonParseException.class, () -> objectMapper.readValue(json, ChatRequestParams.class));
    }

}
