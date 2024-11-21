// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ChatPromptTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String prompt = "Test prompt";
    private final String escapedPrompt = "Test escaped prompt";
    private final String command = "Test command";

    @Test
    void testRecordConstructionAndGetters() {
        ChatPrompt chatPrompt = new ChatPrompt(prompt, escapedPrompt, command);

        assertEquals(prompt, chatPrompt.prompt());
        assertEquals(escapedPrompt, chatPrompt.escapedPrompt());
        assertEquals(command, chatPrompt.command());
    }

    @Test
    void testJsonSerialization() throws Exception {
        ChatPrompt chatPrompt = new ChatPrompt(prompt, escapedPrompt, command);
        String serializedObject = objectMapper.writeValueAsString(chatPrompt);

        assertEquals(serializedObject,
                "{\"prompt\":\"Test prompt\",\"escapedPrompt\":\"Test escaped prompt\",\"command\":\"Test command\"}");
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "{\"prompt\":\"Test prompt\",\"escapedPrompt\":\"Test escaped prompt\",\"command\":\"Test command\"}";

        ChatPrompt deserializedPrompt = objectMapper.readValue(json, ChatPrompt.class);

        assertEquals("Test prompt", deserializedPrompt.prompt());
        assertEquals("Test escaped prompt", deserializedPrompt.escapedPrompt());
        assertEquals("Test command", deserializedPrompt.command());
    }

    @Test
    void testDeserializationException() throws Exception {
        String json = "incorrectly formatted json";

        assertThrows(JsonParseException.class, () -> objectMapper.readValue(json, ChatPrompt.class));
    }

}
