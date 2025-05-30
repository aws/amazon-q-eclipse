// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GenericLinkClickParamsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String tabId = "tabId";
    private final String link = "link";
    private final String eventId = "eventId";
    private final String messageId = "messageId";

    @Test
    void testRecordSettersAndGetters() {
        GenericLinkClickParams genericLinkClickParams = new GenericLinkClickParams();
        genericLinkClickParams.setTabId(tabId);
        genericLinkClickParams.setLink(link);
        genericLinkClickParams.setEventId(eventId);
        genericLinkClickParams.setMessageId(messageId);

        assertEquals(tabId, genericLinkClickParams.getTabId());
        assertEquals(link, genericLinkClickParams.getLink());
        assertEquals(eventId, genericLinkClickParams.getEventId());
        assertEquals(messageId, genericLinkClickParams.getMessageId());
}

    @Test
    void testJsonSerialization() throws Exception {
        GenericLinkClickParams genericLinkClickParams = new GenericLinkClickParams();
        genericLinkClickParams.setTabId(tabId);
        genericLinkClickParams.setLink(link);
        genericLinkClickParams.setEventId(eventId);
        genericLinkClickParams.setMessageId(messageId);

        String serializedObject = objectMapper.writeValueAsString(genericLinkClickParams);

        assertEquals("{\"tabId\":\"tabId\",\"link\":\"link\",\"eventId\":\"eventId\",\"messageId\":\"messageId\"}",
                serializedObject);
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "{\"tabId\":\"tabId\",\"link\":\"link\",\"eventId\":\"eventId\",\"messageId\":\"messageId\"}";

        GenericLinkClickParams deserializedResult = objectMapper.readValue(json, GenericLinkClickParams.class);

        assertEquals(tabId, deserializedResult.getTabId());
        assertEquals(link, deserializedResult.getLink());
        assertEquals(eventId, deserializedResult.getEventId());
        assertEquals(messageId, deserializedResult.getMessageId());
    }

    @Test
    void testJsonDeserializationWithoutMessageId() throws Exception {
        String json = "{\"tabId\":\"tabId\",\"link\":\"link\",\"eventId\":\"eventId\"}";

        GenericLinkClickParams deserializedResult = objectMapper.readValue(json, GenericLinkClickParams.class);

        assertEquals(tabId, deserializedResult.getTabId());
        assertEquals(link, deserializedResult.getLink());
        assertEquals(eventId, deserializedResult.getEventId());
        assertNull(deserializedResult.getMessageId());
    }

    @Test
    void testDeserializationException() throws Exception {
        String json = "incorrectly formatted json";

        assertThrows(JsonParseException.class, () -> objectMapper.readValue(json, GenericLinkClickParams.class));
    }

}
