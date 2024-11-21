// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FeedbackParamsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String tabId = "tabId";
    private final String eventId = "eventId";

    private final String messageId = "messageId";
    private final String selectedOption = "selectedOption";
    private final String comment = "comment";
    private final FeedbackPayload feedbackPayload = new FeedbackPayload(messageId, tabId, selectedOption, comment);

    @Test
    void testRecordConstructionAndGetters() {
        FeedbackParams feedbackParams = new FeedbackParams(tabId, eventId, feedbackPayload);

        assertEquals(tabId, feedbackParams.tabId());
        assertEquals(eventId, feedbackParams.eventId());
        assertNotNull(feedbackParams.feedbackPayload());
    }

    @Test
    void testJsonSerialization() throws Exception {
        FeedbackParams feedbackParams = new FeedbackParams(tabId, eventId, feedbackPayload);

        String serializedObject = objectMapper.writeValueAsString(feedbackParams);

        assertEquals(
                "{\"tabId\":\"tabId\",\"eventId\":\"eventId\",\"feedbackPayload\":{\"messageId\":\"messageId\",\"tabId\":\"tabId\""
                        + ",\"selectedOption\":\"selectedOption\",\"comment\":\"comment\"}}",
                serializedObject);
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "{\"tabId\":\"tabId\",\"eventId\":\"eventId\",\"feedbackPayload\":{\"messageId\":\"messageId\","
                + "\"tabId\":\"tabId\",\"selectedOption\":\"selectedOption\",\"comment\":\"comment\"}}";

        FeedbackParams deserializedResult = objectMapper.readValue(json, FeedbackParams.class);

        assertEquals(tabId, deserializedResult.tabId());
        assertEquals(eventId, deserializedResult.eventId());
        assertNotNull(deserializedResult.feedbackPayload());
    }

    @Test
    void testDeserializationException() throws Exception {
        String json = "incorrectly formatted json";

        assertThrows(JsonParseException.class, () -> objectMapper.readValue(json, FeedbackParams.class));
    }

}
