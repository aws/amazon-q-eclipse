// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FeedbackPayloadTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String messageId = "messageId";
    private final String tabId = "tabId";
    private final String selectedOption = "selectedOption";
    private final String comment = "comment";

    @Test
    void testRecordConstructionAndGetters() {
        FeedbackPayload feedbackPayload = new FeedbackPayload(messageId, tabId, selectedOption, comment);

        assertEquals(messageId, feedbackPayload.messageId());
        assertEquals(tabId, feedbackPayload.tabId());
        assertEquals(selectedOption, feedbackPayload.selectedOption());
        assertEquals(comment, feedbackPayload.comment());
    }

    @Test
    void testJsonSerialization() throws Exception {
        FeedbackPayload feedbackPayload = new FeedbackPayload(messageId, tabId, selectedOption, comment);

        String serializedObject = objectMapper.writeValueAsString(feedbackPayload);

        assertEquals(
                "{\"messageId\":\"messageId\",\"tabId\":\"tabId\",\"selectedOption\":\"selectedOption\",\"comment\":\"comment\"}",
                serializedObject);
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "{\"messageId\":\"messageId\",\"tabId\":\"tabId\",\"selectedOption\":\"selectedOption\",\"comment\":\"comment\"}";

        FeedbackPayload deserializedResult = objectMapper.readValue(json, FeedbackPayload.class);

        assertEquals(messageId, deserializedResult.messageId());
        assertEquals(tabId, deserializedResult.tabId());
        assertEquals(selectedOption, deserializedResult.selectedOption());
        assertEquals(comment, deserializedResult.comment());
    }

    @Test
    void testDeserializationException() throws Exception {
        String json = "incorrectly formatted json";

        assertThrows(JsonParseException.class, () -> objectMapper.readValue(json, FeedbackPayload.class));
    }

}
