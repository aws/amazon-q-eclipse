// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.encryption;

import org.junit.jupiter.api.Test;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LspJsonWebTokenTest {

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

    }

    @Test
    void testEncryptAndDecrypt() {
        TestObject testObject = new TestObject("test data");
        String encryptedData = LspJsonWebToken.encrypt(TestSecretKey.createTestSecretKey(), testObject);
        assertNotEquals(testObject.field, encryptedData);
        String decryptedData = LspJsonWebToken.decrypt(TestSecretKey.createTestSecretKey(), encryptedData);
        assertEquals(decryptedData, "{\"field\":\"test data\"}");
    }

    @Test
    void testEncryptionFailure() {
        SecretKey invalidKey = new SecretKeySpec(new byte[1], "AES");

        assertThrows(AmazonQPluginException.class,
                () -> LspJsonWebToken.encrypt(invalidKey, new TestObject("test data")));
    }

    @Test
    void testDecryptionFailure() {
        String invalidJwtString = "invalid.jwt.string";

        assertThrows(AmazonQPluginException.class,
                () -> LspJsonWebToken.decrypt(TestSecretKey.createTestSecretKey(), invalidJwtString));
    }

}
