// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.lsp.encryption;

import java.io.OutputStream;

import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;

public final class LspEncryptionManager {

    private static LspEncryptionManager instance;
    private final LspEncryptionKey lspEncryptionKey;


    private LspEncryptionManager(final Builder builder) {
        lspEncryptionKey = builder.lspEncryptionKey != null ? builder.lspEncryptionKey : new LspEncryptionKey();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static synchronized LspEncryptionManager getInstance() {
        if (instance == null) {
            try {
                instance = LspEncryptionManager.builder()
                    .build();
            } catch (Exception e) {
                throw new AmazonQPluginException("Failed to initialize LspEncryptionManager", e);
            }
        }
        return instance;
    }

    public String encrypt(final Object data) {
        return LspJsonWebToken.encrypt(lspEncryptionKey.getKey(), data);
    }

    public String decrypt(final String jwt) {
        return LspJsonWebToken.decrypt(lspEncryptionKey.getKey(), jwt);
    }

    /*
     * For improved security, the communication between the Plugin extension and the Amazon Q LSP Server
     * should be encrypted. The Amazon Q LSP Server is initiated as encrypted when the --set-credentials-encryption-key
     * flag is provided in the start up command {@link QLspConnectionProvider}. Once the sub-process for the LSP server
     * has been initiated, it waits for an encryption key to be sent over stdin before initiating the LSP protocol.
     * The server saves the provided encryption key and will use the key for future requests (such as Q Chat Requests)
     */
    public void initializeEncrypedCommunication(final OutputStream serverStdin) {
        // Ensure the message does not contain any newline characters. The server will
        // process characters up to the first newline.
        String message = String.format("""
                {\
                    "version": "1.0", \
                    "key": "%s", \
                    "mode": "JWT" \
                }\
                """, lspEncryptionKey.getKeyAsBase64());

        try {
            serverStdin.write((message + "\n").getBytes());
            serverStdin.flush();
        } catch (Exception e) {
            throw new AmazonQPluginException("Failed to initialize encrypted communication", e);
        }
    }

    public static class Builder {

        private LspEncryptionKey lspEncryptionKey;

        public final Builder withLspEncryptionKey(final LspEncryptionKey lspEncryptionKey) {
            this.lspEncryptionKey = lspEncryptionKey;
            return this;
        }

        public final LspEncryptionManager build() {
            return new LspEncryptionManager(this);
        }
    }

}
