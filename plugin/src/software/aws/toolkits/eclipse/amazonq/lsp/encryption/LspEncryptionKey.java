// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.lsp.encryption;

import java.util.Base64;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;

public final class LspEncryptionKey {
    private SecretKey key;

    public LspEncryptionKey() {
        this.key = generateKey();
    }

    public SecretKey getKey() {
        return key;
    }

    public String getKeyAsBase64() {
        return base64Encode(key);
    }

    private String base64Encode(final SecretKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public static SecretKey generateKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            return keyGen.generateKey();
        } catch (Exception e) {
            throw new AmazonQPluginException("Error occurred while generating LSP encryption key", e);
        }
    }
}
