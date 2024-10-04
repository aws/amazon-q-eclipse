package software.aws.toolkits.eclipse.amazonq.lsp.encryption;

import java.io.OutputStream;

import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;

public final class LspEncryptionManager {

    private static LspEncryptionManager instance;
    private LspEncryptionKey lspEncryptionKey;

    private LspEncryptionManager() {
        lspEncryptionKey = new LspEncryptionKey();
    }

    public static synchronized LspEncryptionManager getInstance() {
        if (instance == null) {
            try {
                instance = new LspEncryptionManager();
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

    public void initializeEncrypedCommunication(final OutputStream serverStdin) {
        // Ensure the message does not contain any newline characters. The server will
        // process characters up
        // to the first newline.
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
}
