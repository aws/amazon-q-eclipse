// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CopyToClipboardParamsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String tabId = "tabId";
    private final String messageId = "messageId";
    private final String code = "code";
    private final String type = "type";

    private final String licenseName = "licenseName";
    private final String repository = "repository";
    private final String url = "url";

    private final Integer startLine = 1;
    private final Integer endLine = 2;
    private final RecommendationContentSpan recommendationSpan = new RecommendationContentSpan(startLine, endLine);

    private final String information = "information";
    private final ReferenceTrackerInformation referenceTrackerInformation = new ReferenceTrackerInformation(licenseName,
            repository, url, recommendationSpan, information);

    private final ReferenceTrackerInformation[] referenceTrackerInformationList = new ReferenceTrackerInformation[] {
            referenceTrackerInformation};

    private final String eventId = "eventId";
    private final Integer codeBlockIndex = 0;
    private final Integer totalCodeBlocks = 1;

    @Test
    void testRecordConstructionAndGetters() {
        CopyToClipboardParams copyToClipboardParams = new CopyToClipboardParams(tabId, messageId, code, type,
                referenceTrackerInformationList, eventId, codeBlockIndex, totalCodeBlocks);

        assertEquals(tabId, copyToClipboardParams.tabId());
        assertEquals(messageId, copyToClipboardParams.messageId());
        assertEquals(code, copyToClipboardParams.code());
        assertEquals(type, copyToClipboardParams.type());
        assertEquals(referenceTrackerInformationList, copyToClipboardParams.referenceTrackerInformation());
        assertEquals(eventId, copyToClipboardParams.eventId());
        assertEquals(codeBlockIndex, copyToClipboardParams.codeBlockIndex());
        assertEquals(totalCodeBlocks, copyToClipboardParams.totalCodeBlocks());
    }

    @Test
    void testJsonSerialization() throws Exception {
        CopyToClipboardParams copyToClipboardParams = new CopyToClipboardParams(tabId, messageId, code, type,
                referenceTrackerInformationList, eventId, codeBlockIndex, totalCodeBlocks);

        String serializedObject = objectMapper.writeValueAsString(copyToClipboardParams);

        assertEquals("{\"tabId\":\"tabId\",\"messageId\":\"messageId\",\"code\":\"code\",\"type\":\"type\",\""
                + "referenceTrackerInformation\":[{\"licenseName\":\"licenseName\",\"repository\":\"repository\","
                + "\"url\":\"url\",\"recommendationContentSpan\":{\"start\":1,\"end\":2},\"information\":"
                + "\"information\"}],\"eventId\":\"eventId\",\"codeBlockIndex\":0,\"totalCodeBlocks\":1}",
                serializedObject);
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "{\"tabId\":\"tabId\",\"messageId\":\"messageId\",\"code\":\"code\",\"type\":\"type\",\""
                + "referenceTrackerInformation\":[{\"licenseName\":\"licenseName\",\"repository\":\"repository\","
                + "\"url\":\"url\",\"recommendationContentSpan\":{\"start\":1,\"end\":2},\"information\":"
                + "\"information\"}],\"eventId\":\"eventId\",\"codeBlockIndex\":0,\"totalCodeBlocks\":1}";

        CopyToClipboardParams deserializedResult = objectMapper.readValue(json, CopyToClipboardParams.class);

        assertEquals(tabId, deserializedResult.tabId());
        assertEquals(messageId, deserializedResult.messageId());
        assertEquals(code, deserializedResult.code());
        assertEquals(type, deserializedResult.type());
        assertEquals(1, deserializedResult.referenceTrackerInformation().length);
        assertNotNull(deserializedResult.referenceTrackerInformation()[0]);
        assertEquals(eventId, deserializedResult.eventId());
        assertEquals(codeBlockIndex, deserializedResult.codeBlockIndex());
        assertEquals(totalCodeBlocks, deserializedResult.totalCodeBlocks());
    }

    @Test
    void testDeserializationException() throws Exception {
        String json = "incorrectly formatted json";

        assertThrows(JsonParseException.class, () -> objectMapper.readValue(json, CopyToClipboardParams.class));
    }

}
