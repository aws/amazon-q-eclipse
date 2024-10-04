package software.aws.toolkits.eclipse.amazonq.lsp.encryption;

import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.KeyLengthException;

import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;

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
    
    public String encode(Object data) throws KeyLengthException, JOSEException {
    	LspJsonWebToken lspJsonWebToken = new LspJsonWebToken(lspEncryptionKey.getKey());
    	String jwt = lspJsonWebToken.encode(data);
    	return jwt;
    }
    
    public String decode(String jwt) {
    	LspJsonWebToken lspJsonWebToken = new LspJsonWebToken(lspEncryptionKey.getKey());
    	return lspJsonWebToken.decode(jwt);
    }

    public void initializeEncrypedCommunication(final OutputStream serverStdin) throws IOException {
    	// Ensure the message does not contain any newline characters. The server will process characters up
    	// to the first newline.
        String message = String.format("""
                {\
                    "version": "1.0", \
                    "key": "%s", \
                    "mode": "JWT" \
                }\
                """, lspEncryptionKey.getKeyAsBase64());
        
        PluginLogger.info("Sending encryption initalization: " + message);
        
        sendMessageToLspServer(serverStdin, message);
    }

    private void sendMessageToLspServer(final OutputStream serverStdin, final String message) throws IOException {
        if (serverStdin != null) {
            serverStdin.write((message + "\n").getBytes());
            serverStdin.flush();
        } else {
            throw new IllegalStateException("Server stdin is not available.");
        }
    }
}
