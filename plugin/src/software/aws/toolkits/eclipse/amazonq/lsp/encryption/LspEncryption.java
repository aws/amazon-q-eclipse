package software.aws.toolkits.eclipse.amazonq.lsp.encryption;

import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;

public final class LspEncryption {

    private static LspEncryption instance;
    private LspEncryptionKey lspEncryptionKey;

    private LspEncryption() throws NoSuchAlgorithmException {
        lspEncryptionKey = new LspEncryptionKey();
    }

    public static synchronized LspEncryption getInstance() throws NoSuchAlgorithmException {
        if (instance == null) {
            instance = new LspEncryption();
        }
        return instance;
    }

    public void initializeEncrypedCommunication(final OutputStream serverStdin) throws IOException {
        // String message = String.format("{\"version\": \"1.0\",\"key\":\"%s\",\"mode\":\"JWT\"}", lspEncryptionKey.getKey());
        String message = String.format("""
                {\
                    "version": "1.0", \
                    "key": "%s", \
                    "mode": "JWT" \
                }\
                """, lspEncryptionKey.getKey());
        sendMessageToServer(serverStdin, message);
    }

    private void sendMessageToServer(final OutputStream serverStdin, final String message) throws IOException {
        if (serverStdin != null) {
            serverStdin.write((message + "\n").getBytes());
            serverStdin.flush();
        } else {
            throw new IllegalStateException("Server stdin is not available. Did you start the server?");
        }
    }
}
