// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GenericTabParamsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String tabId = "tabId";

    @Test
    void testRecordConstructionAndGetters() {
        GenericTabParams genericTabParams = new GenericTabParams(tabId);

        assertEquals(tabId, genericTabParams.tabId());
    }

    @Test
    void testJsonSerialization() throws Exception {
        GenericTabParams genericTabParams = new GenericTabParams(tabId);

        String serializedObject = objectMapper.writeValueAsString(genericTabParams);

        assertEquals("{\"tabId\":\"tabId\"}", serializedObject);
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "{\"tabId\":\"tabId\"}";

        GenericTabParams deserializedResult = objectMapper.readValue(json, GenericTabParams.class);

        assertEquals(tabId, deserializedResult.tabId());
    }

    @Test
    void testDeserializationException() throws Exception {
        String json = "incorrectly formatted json";

        assertThrows(JsonParseException.class, () -> objectMapper.readValue(json, GenericTabParams.class));
    }

}
