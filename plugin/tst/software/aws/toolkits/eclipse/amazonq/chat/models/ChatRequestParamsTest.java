// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ChatRequestParamsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String tabId = "tabId";

    private final String prompt = "prompt";
    private final String escapedPrompt = "escapedPrompt";
    private final String command = "command";
    private final ChatPrompt chatPrompt = new ChatPrompt(prompt, escapedPrompt, command, Collections.emptyList());

    private final String partialResultToken = "partialResultToken";

    private final String textDocumentUri = "test/file.txt";
    private final TextDocumentIdentifier textDocumentIdentifier = new TextDocumentIdentifier(textDocumentUri);

    private final Position startPosition = new Position(0, 0);
    private final Position endPosition = new Position(100, 100);
    private final Range range = new Range(startPosition, endPosition);
    private final CursorState cursorState = new CursorState(range);
    private final List<CursorState> cursorStateList = List.of(cursorState);
    @Test
    void testSettersGetters() {
        ChatRequestParams chatRequestParams = new ChatRequestParams(tabId, chatPrompt,
                new TextDocumentIdentifier("newDocumentUri"),
                List.of(new CursorState(
                        new Range(startPosition, endPosition))), Collections.emptyList());

        assertEquals(tabId, chatRequestParams.getTabId());
        assertEquals(chatPrompt, chatRequestParams.getPrompt());
        assertNotNull(chatRequestParams.getTextDocument());
        assertNotNull(chatRequestParams.getCursorState());

        chatRequestParams.setPartialResultToken(partialResultToken);
        chatRequestParams.setCursorState(cursorStateList);
        chatRequestParams.setTextDocument(textDocumentIdentifier);

        assertEquals(partialResultToken, chatRequestParams.getPartialResultToken());
        assertEquals(cursorStateList, chatRequestParams.getCursorState());
        assertEquals(textDocumentIdentifier, chatRequestParams.getTextDocument());
    }

    @Test
    void testDeserialization() {
        String json = """
                {
                    "tabId": "tabId",
                    "prompt": {
                        "prompt": "prompt",
                        "escapedPrompt": "escapedPrompt",
                        "command": "command"
                    },
                    "textDocument": {
                        "uri": "test/file.txt"
                    },
                    "cursorState": [
                        {
                            "range": {
                                "start": {
                                    "line": 0,
                                    "character": 0
                                },
                                "end": {
                                    "line": 100,
                                    "character": 100
                                }
                            }
                        }
                    ]
                }""";

        ChatRequestParams params = assertDoesNotThrow(() -> objectMapper.readValue(json, ChatRequestParams.class));

        assertEquals(tabId, params.getTabId());
        assertNotNull(params.getPrompt());
        assertEquals(textDocumentUri, params.getTextDocument().getUri());
        assertEquals(1, params.getCursorState().size());
        assertNotNull(params.getCursorState().get(0));
    }

    @Test
    void testDeserializationException() throws Exception {
        String json = "incorrectly formatted json";

        assertThrows(JsonParseException.class, () -> objectMapper.readValue(json, ChatRequestParams.class));
    }
}
