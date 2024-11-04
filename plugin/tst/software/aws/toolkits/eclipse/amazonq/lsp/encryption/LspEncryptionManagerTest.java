// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.extensions.implementation.LspJsonWebTokenStaticMockExtension;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public final class LspEncryptionManagerTest {

    @RegisterExtension
    private static LspJsonWebTokenStaticMockExtension lspJsonWebTokenStaticMockExtension
            = new LspJsonWebTokenStaticMockExtension();

    private LspEncryptionKey lspEncryptionKeyMock;
    private LspEncryptionManager lspEncryptionManager;
    private SecretKey secretKey;

    private final class TestObject {
        private String field;
        TestObject(final String field) {
            this.field = field;
        }
        public String getField() {
            return field;
        }
    }

    private final class TestSecretKey {

        private TestSecretKey() { }

        public static SecretKeySpec createTestSecretKey() {
            byte[] keyBytes = new byte[32];
            for (int i = 0; i < keyBytes.length; i++) {
                keyBytes[i] = (byte) i;
            }
            return new SecretKeySpec(keyBytes, "AES");
        }

        public static String getKeyAsBase64(final SecretKeySpec secretKey) {
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        }

    }

    @BeforeEach
    void setupBeforeEach() {
        lspEncryptionKeyMock = mock(LspEncryptionKey.class);
        lspEncryptionManager = LspEncryptionManager.builder()
                .withLspEncryptionKey(lspEncryptionKeyMock)
                .build();
    }

    @Test
    void testEncryptCallsLspJsonWebTokenMethod() {
        SecretKey testSecretKey = TestSecretKey.createTestSecretKey();
        TestObject testObject = new TestObject("test");

        when(lspEncryptionKeyMock.getKey()).thenReturn(testSecretKey);

        String result = lspEncryptionManager.encrypt(testObject);

        MockedStatic<LspJsonWebToken> lspJsonWebTokenStaticMock = lspJsonWebTokenStaticMockExtension.getStaticMock();
        lspJsonWebTokenStaticMock.verify(() -> LspJsonWebToken.encrypt(any(SecretKey.class), any(Object.class)),
                times(1));

        assertEquals(result, "Mocked Encrypted Value");
    }

    @Test
    void testDecryptCallsLspJsonWebTokenMethod() {
        SecretKey testSecretKey = TestSecretKey.createTestSecretKey();

        when(lspEncryptionKeyMock.getKey()).thenReturn(testSecretKey);

        String result = lspEncryptionManager.decrypt("test");

        MockedStatic<LspJsonWebToken> lspJsonWebTokenStaticMock = lspJsonWebTokenStaticMockExtension.getStaticMock();
        lspJsonWebTokenStaticMock.verify(() -> LspJsonWebToken.decrypt(any(SecretKey.class), any(String.class)),
                times(1));

        assertEquals(result, "Mocked Decrypted Value");
    }

    @Test
    void testInitializeEncryptedCommunicationGoldenPath() throws IOException {
        OutputStream outputStream = Mockito.mock(OutputStream.class);

        String encodedKey = TestSecretKey.getKeyAsBase64(TestSecretKey.createTestSecretKey());
        when(lspEncryptionKeyMock.getKeyAsBase64()).thenReturn(encodedKey);

        String expectedResult = String.format("""
                {\
                    "version": "1.0", \
                    "key": "%s", \
                    "mode": "JWT" \
                }\
                """, encodedKey);

        assertDoesNotThrow(() -> lspEncryptionManager.initializeEncrypedCommunication(outputStream));
        verify(outputStream, times(1)).write((expectedResult + "\n").getBytes());
        verify(outputStream, times(1)).flush();
    }

    @Test
    void testInitializeEncryptedCommunicationException() throws IOException {
        OutputStream outputStream = Mockito.mock(OutputStream.class);

        String encodedKey = TestSecretKey.getKeyAsBase64(TestSecretKey.createTestSecretKey());
        when(lspEncryptionKeyMock.getKeyAsBase64()).thenReturn(encodedKey);

        doThrow(new IOException())
                .when(outputStream).write(any());

        assertThrows(AmazonQPluginException.class,
                () -> lspEncryptionManager.initializeEncrypedCommunication(outputStream));
    }

}
