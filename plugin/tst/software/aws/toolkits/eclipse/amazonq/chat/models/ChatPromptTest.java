// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.aws.toolkits.eclipse.amazonq.lsp.model.Command;

public class ChatPromptTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String prompt = "Test prompt";
    private final String escapedPrompt = "Test escaped prompt";
    private final String command = "Test command";

    @Test
    void testRecordConstructionAndGetters() {
        ChatPrompt chatPrompt = new ChatPrompt(prompt, escapedPrompt, command, Collections.emptyList());

        assertEquals(prompt, chatPrompt.prompt());
        assertEquals(escapedPrompt, chatPrompt.escapedPrompt());
        assertEquals(command, chatPrompt.command());
    }

    @Test
    void testJsonSerialization() throws Exception {
        ChatPrompt chatPrompt = new ChatPrompt(prompt, escapedPrompt, command, Collections.singletonList(new Command("foo", "bar")));
        String serializedObject = objectMapper.writeValueAsString(chatPrompt);

        assertEquals(serializedObject,
                "{\"prompt\":\"Test prompt\",\"escapedPrompt\":\"Test escaped prompt\","
                + "\"command\":\"Test command\",\"context\":[{\"command\":\"foo\",\"description\":\"bar\"}]}");
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "{\"prompt\":\"Test prompt\",\"escapedPrompt\":\"Test escaped prompt\","
                + "\"command\":\"Test command\",\"context\":[{\"command\":\"foo\",\"description\":\"bar\"}]}";

        ChatPrompt deserializedPrompt = objectMapper.readValue(json, ChatPrompt.class);

        assertEquals("Test prompt", deserializedPrompt.prompt());
        assertEquals("Test escaped prompt", deserializedPrompt.escapedPrompt());
        assertEquals("Test command", deserializedPrompt.command());
        assertEquals("[{\"command\":\"foo\",\"description\":\"bar\"}]", objectMapper.writeValueAsString(deserializedPrompt.context()));
    }

    @Test
    void testDeserializationException() throws Exception {
        String json = "incorrectly formatted json";

        assertThrows(JsonParseException.class, () -> objectMapper.readValue(json, ChatPrompt.class));
    }

}
