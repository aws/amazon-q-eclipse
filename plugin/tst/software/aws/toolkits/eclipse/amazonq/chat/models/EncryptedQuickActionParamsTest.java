// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EncryptedQuickActionParamsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String message = "message";
    private final String partialResultToken = "partialResultToken";

    @Test
    void testRecordConstructionAndGetters() {
        EncryptedQuickActionParams encryptedQuickActionParams = new EncryptedQuickActionParams(message,
                partialResultToken);

        assertEquals(message, encryptedQuickActionParams.message());
        assertEquals(partialResultToken, encryptedQuickActionParams.partialResultToken());
    }

    @Test
    void testJsonSerialization() throws Exception {
        EncryptedQuickActionParams encryptedQuickActionParams = new EncryptedQuickActionParams(message,
                partialResultToken);

        String serializedObject = objectMapper.writeValueAsString(encryptedQuickActionParams);

        assertEquals("{\"message\":\"message\",\"partialResultToken\":\"partialResultToken\"}", serializedObject);
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "{\"message\":\"message\",\"partialResultToken\":\"partialResultToken\"}";

        EncryptedQuickActionParams deserializedResult = objectMapper.readValue(json, EncryptedQuickActionParams.class);

        assertEquals(message, deserializedResult.message());
        assertEquals(partialResultToken, deserializedResult.partialResultToken());
    }

    @Test
    void testDeserializationException() throws Exception {
        String json = "incorrectly formatted json";

        assertThrows(JsonParseException.class, () -> objectMapper.readValue(json, EncryptedQuickActionParams.class));
    }

}
