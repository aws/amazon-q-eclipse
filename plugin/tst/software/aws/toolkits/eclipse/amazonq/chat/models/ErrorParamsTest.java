// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ErrorParamsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String tabId = "tabId";
    private final String triggerType = "triggerType";
    private final String message = "message";
    private final String title = "title";

    @Test
    void testRecordConstructionAndGetters() {
        ErrorParams errorParams = new ErrorParams(tabId, triggerType, message, title);

        assertEquals(tabId, errorParams.tabId());
        assertEquals(triggerType, errorParams.triggerType());
        assertEquals(message, errorParams.message());
        assertEquals(title, errorParams.title());
    }

    @Test
    void testJsonSerialization() throws Exception {
        ErrorParams errorParams = new ErrorParams(tabId, triggerType, message, title);

        String serializedObject = objectMapper.writeValueAsString(errorParams);

        assertEquals(
                "{\"tabId\":\"tabId\",\"triggerType\":\"triggerType\",\"message\":\"message\",\"title\":\"title\"}",
                serializedObject);
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "{\"tabId\":\"tabId\",\"triggerType\":\"triggerType\",\"message\":\"message\",\"title\":\"title\"}";

        ErrorParams deserializedResult = objectMapper.readValue(json, ErrorParams.class);

        assertEquals(tabId, deserializedResult.tabId());
        assertEquals(triggerType, deserializedResult.triggerType());
        assertEquals(message, deserializedResult.message());
        assertEquals(title, deserializedResult.title());
    }

    @Test
    void testDeserializationException() throws Exception {
        String json = "incorrectly formatted json";

        assertThrows(JsonParseException.class, () -> objectMapper.readValue(json, ErrorParams.class));
    }

}
