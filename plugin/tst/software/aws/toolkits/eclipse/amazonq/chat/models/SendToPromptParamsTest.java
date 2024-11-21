// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SendToPromptParamsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String selection = "selection";
    private final String triggerType = "triggerType";

    @Test
    void testRecordConstructionAndGetters() {
        SendToPromptParams sendToPromptParams = new SendToPromptParams(selection, triggerType);

        assertEquals(selection, sendToPromptParams.selection());
        assertEquals(triggerType, sendToPromptParams.triggerType());
    }

    @Test
    void testJsonSerialization() throws Exception {
        SendToPromptParams sendToPromptParams = new SendToPromptParams(selection, triggerType);

        String serializedObject = objectMapper.writeValueAsString(sendToPromptParams);

        assertEquals("{\"selection\":\"selection\",\"triggerType\":\"triggerType\"}", serializedObject);
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "{\"selection\":\"selection\",\"triggerType\":\"triggerType\"}";

        SendToPromptParams deserializedPrompt = objectMapper.readValue(json, SendToPromptParams.class);

        assertEquals(selection, deserializedPrompt.selection());
        assertEquals(triggerType, deserializedPrompt.triggerType());
    }

    @Test
    void testDeserializationException() throws Exception {
        String json = "incorrectly formatted json";

        assertThrows(JsonParseException.class, () -> objectMapper.readValue(json, SendToPromptParams.class));
    }

}
