// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GenericCommandParamsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String tabId = "tabId";
    private final String selection = "selection";
    private final String triggerType = "triggerType";
    private final String genericCommand = "genericCommand";

    @Test
    void testRecordConstructionAndGetters() {
        GenericCommandParams genericCommandParams = new GenericCommandParams(tabId, selection, triggerType,
                genericCommand);

        assertEquals(tabId, genericCommandParams.tabId());
        assertEquals(selection, genericCommandParams.selection());
        assertEquals(triggerType, genericCommandParams.triggerType());
        assertEquals(genericCommand, genericCommandParams.genericCommand());
    }

    @Test
    void testJsonSerialization() throws Exception {
        GenericCommandParams genericCommandParams = new GenericCommandParams(tabId, selection, triggerType,
                genericCommand);

        String serializedObject = objectMapper.writeValueAsString(genericCommandParams);

        assertEquals(
                "{\"tabId\":\"tabId\",\"selection\":\"selection\",\"triggerType\":\"triggerType\",\"genericCommand\":\"genericCommand\"}",
                serializedObject);
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "{\"tabId\":\"tabId\",\"selection\":\"selection\",\"triggerType\":\"triggerType\",\"genericCommand\":\"genericCommand\"}";

        GenericCommandParams deserializedResult = objectMapper.readValue(json, GenericCommandParams.class);

        assertEquals(tabId, deserializedResult.tabId());
        assertEquals(selection, deserializedResult.selection());
        assertEquals(triggerType, deserializedResult.triggerType());
        assertEquals(genericCommand, deserializedResult.genericCommand());
    }

    @Test
    void testDeserializationException() throws Exception {
        String json = "incorrectly formatted json";

        assertThrows(JsonParseException.class, () -> objectMapper.readValue(json, GenericCommandParams.class));
    }

}
