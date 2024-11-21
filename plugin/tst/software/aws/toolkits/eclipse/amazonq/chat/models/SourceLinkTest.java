// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SourceLinkTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String title = "title";
    private final String url = "url";
    private final String body = "body";

    @Test
    void testRecordConstructionAndGetters() {
        SourceLink sourceLink = new SourceLink(title, url, body);

        assertEquals(title, sourceLink.title());
        assertEquals(url, sourceLink.url());
        assertEquals(body, sourceLink.body());
    }

    @Test
    void testJsonSerialization() throws Exception {
        SourceLink sourceLink = new SourceLink(title, url, body);

        String serializedObject = objectMapper.writeValueAsString(sourceLink);

        assertEquals("{\"title\":\"title\",\"url\":\"url\",\"body\":\"body\"}", serializedObject);
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "{\"title\":\"title\",\"url\":\"url\",\"body\":\"body\"}";

        SourceLink deserializedResult = objectMapper.readValue(json, SourceLink.class);

        assertEquals(title, deserializedResult.title());
        assertEquals(url, deserializedResult.url());
        assertEquals(body, deserializedResult.body());
    }

    @Test
    void testDeserializationException() throws Exception {
        String json = "incorrectly formatted json";

        assertThrows(JsonParseException.class, () -> objectMapper.readValue(json, SourceLink.class));
    }

}
