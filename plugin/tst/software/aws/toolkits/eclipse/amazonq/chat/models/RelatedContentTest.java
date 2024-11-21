// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RelatedContentTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String title = "title";
    private final SourceLink sourceLink = new SourceLink("title", "url", "body");
    private final SourceLink[] sourceLinkArray = new SourceLink[] {sourceLink};

    @Test
    void testRecordConstructionAndGetters() {
        RelatedContent relatedContent = new RelatedContent(title, sourceLinkArray);

        assertEquals(title, relatedContent.title());
        assertEquals(1, relatedContent.content().length);
        assertEquals(sourceLink, relatedContent.content()[0]);
    }

    @Test
    void testJsonSerialization() throws Exception {
        RelatedContent relatedContent = new RelatedContent(title, sourceLinkArray);

        String serializedObject = objectMapper.writeValueAsString(relatedContent);

        assertEquals("{\"title\":\"title\",\"content\":[{\"title\":\"title\",\"url\":\"url\",\"body\":\"body\"}]}",
                serializedObject);
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "{\"title\":\"title\",\"content\":[{\"title\":\"title\",\"url\":\"url\",\"body\":\"body\"}]}";

        RelatedContent deserializedResult = objectMapper.readValue(json, RelatedContent.class);

        assertEquals(title, deserializedResult.title());
        assertEquals(1, deserializedResult.content().length);
        assertNotNull(deserializedResult.content()[0]);
    }

    @Test
    void testDeserializationException() throws Exception {
        String json = "incorrectly formatted json";

        assertThrows(JsonParseException.class, () -> objectMapper.readValue(json, RelatedContent.class));
    }

}
