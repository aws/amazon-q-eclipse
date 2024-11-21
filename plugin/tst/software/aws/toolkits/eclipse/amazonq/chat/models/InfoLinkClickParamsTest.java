// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class InfoLinkClickParamsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String tabId = "tabId";
    private final String link = "link";
    private final String eventId = "eventId";

    @Test
    void testRecordSettersAndGetters() {
        InfoLinkClickParams infoLinkClickParams = new InfoLinkClickParams();
        infoLinkClickParams.setTabId(tabId);
        infoLinkClickParams.setLink(link);
        infoLinkClickParams.setEventId(eventId);

        assertEquals(tabId, infoLinkClickParams.getTabId());
        assertEquals(link, infoLinkClickParams.getLink());
        assertEquals(eventId, infoLinkClickParams.getEventId());
    }

    @Test
    void testJsonSerialization() throws Exception {
        InfoLinkClickParams infoLinkClickParams = new InfoLinkClickParams();
        infoLinkClickParams.setTabId(tabId);
        infoLinkClickParams.setLink(link);
        infoLinkClickParams.setEventId(eventId);

        String serializedObject = objectMapper.writeValueAsString(infoLinkClickParams);

        assertEquals("{\"tabId\":\"tabId\",\"link\":\"link\",\"eventId\":\"eventId\"}", serializedObject);
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "{\"tabId\":\"tabId\",\"link\":\"link\",\"eventId\":\"eventId\"}";

        InfoLinkClickParams deserializedResult = objectMapper.readValue(json, InfoLinkClickParams.class);

        assertEquals(tabId, deserializedResult.getTabId());
        assertEquals(link, deserializedResult.getLink());
        assertEquals(eventId, deserializedResult.getEventId());
    }

    @Test
    void testDeserializationException() throws Exception {
        String json = "incorrectly formatted json";

        assertThrows(JsonParseException.class, () -> objectMapper.readValue(json, InfoLinkClickParams.class));
    }

}
