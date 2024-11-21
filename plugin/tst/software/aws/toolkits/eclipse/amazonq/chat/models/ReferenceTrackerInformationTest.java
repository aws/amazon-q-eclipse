// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ReferenceTrackerInformationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String licenseName = "licenseName";
    private final String repository = "repository";
    private final String url = "url";

    private final Integer startLine = 1;
    private final Integer endLine = 2;
    private final RecommendationContentSpan recommendationSpan = new RecommendationContentSpan(startLine, endLine);

    private final String information = "information";

    @Test
    void testRecordConstructionAndGetters() {
        ReferenceTrackerInformation referenceTrackerInformation = new ReferenceTrackerInformation(licenseName,
                repository, url, recommendationSpan, information);

        assertEquals(licenseName, referenceTrackerInformation.licenseName());
        assertEquals(repository, referenceTrackerInformation.repository());
        assertEquals(url, referenceTrackerInformation.url());
        assertEquals(recommendationSpan, referenceTrackerInformation.recommendationContentSpan());
        assertEquals(information, referenceTrackerInformation.information());
    }

    @Test
    void testJsonSerialization() throws Exception {
        ReferenceTrackerInformation referenceTrackerInformation = new ReferenceTrackerInformation(licenseName,
                repository, url, recommendationSpan, information);

        String serializedObject = objectMapper.writeValueAsString(referenceTrackerInformation);

        assertEquals(
                "{\"licenseName\":\"licenseName\",\"repository\":\"repository\",\"url\":\"url\",\"recommendationContentSpan\":{\"start\""
                        + ":1,\"end\":2},\"information\":\"information\"}",
                serializedObject);
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "{\"licenseName\":\"licenseName\",\"repository\":\"repository\",\"url\":\"url\",\"recommendationContentSpan\":{\"start\""
                + ":1,\"end\":2},\"information\":\"information\"}";

        ReferenceTrackerInformation deserializedResult = objectMapper.readValue(json,
                ReferenceTrackerInformation.class);

        assertEquals(licenseName, deserializedResult.licenseName());
        assertEquals(repository, deserializedResult.repository());
        assertEquals(url, deserializedResult.url());
        assertEquals(recommendationSpan, deserializedResult.recommendationContentSpan());
        assertEquals(information, deserializedResult.information());
    }

    @Test
    void testDeserializationException() throws Exception {
        String json = "incorrectly formatted json";

        assertThrows(JsonParseException.class, () -> objectMapper.readValue(json, ReferenceTrackerInformation.class));
    }

}
