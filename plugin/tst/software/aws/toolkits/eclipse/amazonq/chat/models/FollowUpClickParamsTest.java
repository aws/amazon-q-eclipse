// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FollowUpClickParamsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String tabId = "tabId";
    private final String messageId = "messageId";

    private final String pillText = "Click me";
    private final String prompt = "Test prompt";
    private final Boolean disabled = false;
    private final String description = "Test description";
    private final String type = "button";
    private final ChatItemAction chatItemAction = new ChatItemAction(pillText, prompt, disabled, description, type);

    @Test
    void testRecordConstructionAndGetters() {
        FollowUpClickParams followUpClickParams = new FollowUpClickParams(tabId, messageId, chatItemAction);

        assertEquals(tabId, followUpClickParams.tabId());
        assertEquals(messageId, followUpClickParams.messageId());
        assertNotNull(followUpClickParams.followUp());
    }

    @Test
    void testJsonSerialization() throws Exception {
        FollowUpClickParams followUpClickParams = new FollowUpClickParams(tabId, messageId, chatItemAction);

        String serializedObject = objectMapper.writeValueAsString(followUpClickParams);

        assertEquals(
                "{\"tabId\":\"tabId\",\"messageId\":\"messageId\",\"followUp\":{\"pillText\":\"Click me\",\"prompt\":\"Test prompt\",\"disabled\""
                        + ":false,\"description\":\"Test description\",\"type\":\"button\"}}",
                serializedObject);
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "{\"tabId\":\"tabId\",\"messageId\":\"messageId\",\"followUp\":{\"pillText\":\"Click me\",\"prompt\":\"Test prompt\",\"disabled\""
                + ":false,\"description\":\"Test description\",\"type\":\"button\"}}";

        FollowUpClickParams deserializedResult = objectMapper.readValue(json, FollowUpClickParams.class);

        assertEquals(tabId, deserializedResult.tabId());
        assertEquals(messageId, deserializedResult.messageId());
        assertNotNull(deserializedResult.followUp());
    }

    @Test
    void testDeserializationException() throws Exception {
        String json = "incorrectly formatted json";

        assertThrows(JsonParseException.class, () -> objectMapper.readValue(json, FollowUpClickParams.class));
    }

}
