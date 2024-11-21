// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class InsertToCursorPositionParamsTest {

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

    private final String textDocumentUri = "test/file.txt";
    private final TextDocumentIdentifier textDocumentIdentifier = new TextDocumentIdentifier(textDocumentUri);

    private final String information = "information";
    private final ReferenceTrackerInformation referenceTrackerInformation = new ReferenceTrackerInformation(licenseName,
            repository, url, recommendationSpan, information);

    private final ReferenceTrackerInformation[] referenceTrackerInformationList = new ReferenceTrackerInformation[] {
            referenceTrackerInformation};

    private final String eventId = "eventId";
    private final Integer codeBlockIndex = 0;
    private final Integer totalCodeBlocks = 1;

    private final Position startPosition = new Position(0, 0);
    private final Position endPosition = new Position(100, 100);
    private final Range range = new Range(startPosition, endPosition);
    private final CursorState cursorState = new CursorState(range);
    private final List<CursorState> cursorStateList = List.of(cursorState);

    @Test
    void testRecordConstructionAndGetters() {
        InsertToCursorPositionParams insertToCursorPositionParams = new InsertToCursorPositionParams(tabId, messageId,
                code, type, referenceTrackerInformationList, eventId, codeBlockIndex, totalCodeBlocks,
                textDocumentIdentifier, cursorState);
        insertToCursorPositionParams.setCursorState(cursorStateList);
        insertToCursorPositionParams.setTextDocument(textDocumentIdentifier);

        assertEquals(code, insertToCursorPositionParams.getCode());
        assertEquals(cursorStateList, insertToCursorPositionParams.getCursorState());
        assertEquals(textDocumentIdentifier, insertToCursorPositionParams.getTextDocument());
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "{\"tabId\":\"tabId\",\"messageId\":\"messageId\",\"code\":\"code\",\"type\":\"type\",\""
                + "referenceTrackerInformation\":[{\"licenseName\":\"licenseName\",\"repository\":\"repository\","
                + "\"url\":\"url\",\"recommendationContentSpan\":{\"start\":1,\"end\":2},\"information\":"
                + "\"information\"}],\"eventId\":\"eventId\",\"codeBlockIndex\":0,\"totalCodeBlocks\":1,"
                + "\"textDocument\": {\"uri\": \"test/file.txt\"},\"cursorState\": {\"range\": {\"start\": {"
                + "\"line\": 0,\"character\": 0},\"end\": {\"line\": 100,\"character\": 100}}}}";

        InsertToCursorPositionParams deserializedResult = objectMapper.readValue(json,
                InsertToCursorPositionParams.class);

        assertEquals(code, deserializedResult.getCode());
        assertNull(deserializedResult.getCursorState());
        assertNull(deserializedResult.getTextDocument());
    }

    @Test
    void testDeserializationException() throws Exception {
        String json = "incorrectly formatted json";

        assertThrows(JsonParseException.class, () -> objectMapper.readValue(json, InsertToCursorPositionParams.class));
    }

}
