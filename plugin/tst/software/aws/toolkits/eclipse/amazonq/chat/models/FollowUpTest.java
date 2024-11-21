// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FollowUpTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String pillText = "Click me";
    private final String prompt = "Test prompt";
    private final Boolean disabled = false;
    private final String description = "Test description";
    private final String type = "button";
    private final ChatItemAction chatItemAction = new ChatItemAction(pillText, prompt, disabled, description, type);

    private final String text = "text";
    private final ChatItemAction[] options = new ChatItemAction[] {chatItemAction};

    @Test
    void testRecordConstructionAndGetters() {
        FollowUp followUp = new FollowUp(text, options);

        assertEquals(text, followUp.text());
        assertEquals(1, followUp.options().length);
        assertNotNull(followUp.options()[0]);
    }

    @Test
    void testJsonSerialization() throws Exception {
        FollowUp followUp = new FollowUp(text, options);

        String serializedObject = objectMapper.writeValueAsString(followUp);

        assertEquals(
                "{\"text\":\"text\",\"options\":[{\"pillText\":\"Click me\",\"prompt\":\"Test prompt\",\"disabled\":false,"
                        + "\"description\":\"Test description\",\"type\":\"button\"}]}",
                serializedObject);
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "{\"text\":\"text\",\"options\":[{\"pillText\":\"Click me\",\"prompt\":\"Test prompt\",\"disabled\":false,"
                + "\"description\":\"Test description\",\"type\":\"button\"}]}";

        FollowUp deserializedResult = objectMapper.readValue(json, FollowUp.class);

        assertEquals(text, deserializedResult.text());
        assertEquals(1, deserializedResult.options().length);
        assertNotNull(deserializedResult.options()[0]);
    }

    @Test
    void testDeserializationException() throws Exception {
        String json = "incorrectly formatted json";

        assertThrows(JsonParseException.class, () -> objectMapper.readValue(json, FollowUp.class));
    }

}
