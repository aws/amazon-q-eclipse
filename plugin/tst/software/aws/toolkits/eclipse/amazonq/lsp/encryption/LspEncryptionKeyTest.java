// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

public final class LspEncryptionKeyTest {
    private LspEncryptionKey lspEncryptionKey;

    @BeforeEach
    void setupBeforeEach() {
        lspEncryptionKey = new LspEncryptionKey();
    }

    @Test
    void testKeyGeneration() {
        SecretKey key = lspEncryptionKey.getKey();

        assertNotNull(key, "Generated key should not be null");
        assertEquals("AES", key.getAlgorithm(), "Key algorithm should be AES");
        assertEquals(32, key.getEncoded().length, "Key length should be 32 bytes");
    }

    @Test
    void testKeyAsBase64() {
        String base64Key = lspEncryptionKey.getKeyAsBase64();
        assertNotNull(base64Key, "Base64 encoded key should not be null");
        assertFalse(base64Key.isEmpty(), "Base64 encoded key should not be empty");

        byte[] decodedKey = Base64.getDecoder().decode(base64Key);
        assertArrayEquals(lspEncryptionKey.getKey().getEncoded(), decodedKey,
                "Decoded Base64 key should match the original key");
    }

    @Test
    void testMultipleInstancesGenerateDifferentKey() {
        LspEncryptionKey anotherKey = new LspEncryptionKey();
        assertNotEquals(lspEncryptionKey.getKeyAsBase64(), anotherKey.getKeyAsBase64(),
                "Different instances of LspEncryptionKey should generate different keys");
    }

    @Test
    void testStaticGenerateKey() {
        SecretKey key = LspEncryptionKey.generateKey();
        assertNotNull(key, "Generated key should not be null");
        assertEquals("AES", key.getAlgorithm(), "Key algorithm should be AES");
        assertEquals(256 / 8, key.getEncoded().length, "Key length should be 256 bits (32 bytes)");
    }

    @Test
    void testStaticGenerateKeyExceptionHandling() {
        try (MockedStatic<KeyGenerator> keyGeneratorStaticMock = mockStatic(KeyGenerator.class)) {
            KeyGenerator keyGeneratorMock = mock(KeyGenerator.class);
            keyGeneratorStaticMock.when(() -> KeyGenerator.getInstance("AES"))
                    .thenReturn(keyGeneratorMock);

            doThrow(new RuntimeException("Test exception"))
                    .when(keyGeneratorMock).generateKey();

            assertThrows(AmazonQPluginException.class, LspEncryptionKey::generateKey);
        }
    }

}
