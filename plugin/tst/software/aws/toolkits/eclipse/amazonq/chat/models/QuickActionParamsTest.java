// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class QuickActionParamsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String tabId = "tabId";
    private final String quickAction = "quickAction";
    private final String prompt = "prompt";
    private final String partialResultToken = "partialResultToken";

    @Test
    void testRecordConstructionAndGetters() {
        QuickActionParams quickActionParams = new QuickActionParams(tabId, quickAction, prompt);
        quickActionParams.setPartialResultToken(partialResultToken);

        assertEquals(tabId, quickActionParams.getTabId());
        assertEquals(quickAction, quickActionParams.getQuickAction());
        assertEquals(prompt, quickActionParams.getPrompt());
        assertEquals(partialResultToken, quickActionParams.getPartialResultToken());
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "{\"tabId\":\"tabId\",\"quickAction\":\"quickAction\",\"prompt\":\"prompt\",\"partialResultToken\":\"partialResultToken\"}";

        QuickActionParams deserializedResult = objectMapper.readValue(json, QuickActionParams.class);

        assertEquals(tabId, deserializedResult.getTabId());
        assertEquals(quickAction, deserializedResult.getQuickAction());
        assertEquals(prompt, deserializedResult.getPrompt());
    }

    @Test
    void testDeserializationException() throws Exception {
        String json = "incorrectly formatted json";

        assertThrows(JsonParseException.class, () -> objectMapper.readValue(json, QuickActionParams.class));
    }

}
