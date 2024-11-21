// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CursorStateTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Position startPosition = new Position(0, 0);
    private final Position endPosition = new Position(100, 100);
    private final Range range = new Range(startPosition, endPosition);

    @Test
    void testRecordConstructionAndGetters() {
        CursorState cursorState = new CursorState(range);

        assertEquals(startPosition.getLine(), cursorState.range().getStart().getLine());
        assertEquals(startPosition.getCharacter(), cursorState.range().getStart().getCharacter());
        assertEquals(endPosition.getLine(), cursorState.range().getEnd().getLine());
        assertEquals(endPosition.getCharacter(), cursorState.range().getEnd().getCharacter());
    }

    @Test
    void testJsonSerialization() throws Exception {
        CursorState cursorState = new CursorState(range);

        String serializedObject = objectMapper.writeValueAsString(cursorState);

        assertEquals("{\"range\":{\"start\":{\"line\":0,\"character\":0},\"end\":{\"line\":100,\"character\":100}}}",
                serializedObject);
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "{\"range\":{\"start\":{\"line\":0,\"character\":0},\"end\":{\"line\":100,\"character\":100}}}";

        CursorState deserializedResult = objectMapper.readValue(json, CursorState.class);

        assertEquals(startPosition.getLine(), deserializedResult.range().getStart().getLine());
        assertEquals(startPosition.getCharacter(), deserializedResult.range().getStart().getCharacter());
        assertEquals(endPosition.getLine(), deserializedResult.range().getEnd().getLine());
        assertEquals(endPosition.getCharacter(), deserializedResult.range().getEnd().getCharacter());
    }

    @Test
    void testDeserializationException() throws Exception {
        String json = "incorrectly formatted json";

        assertThrows(JsonParseException.class, () -> objectMapper.readValue(json, CursorState.class));
    }

}
