package software.aws.toolkits.eclipse.amazonq.lsp.encryption;

import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.KeyLengthException;

import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;

public final class LspEncryptionManager {

    private static LspEncryptionManager instance;
    private LspEncryptionKey lspEncryptionKey;

    private LspEncryptionManager() throws NoSuchAlgorithmException {
        lspEncryptionKey = new LspEncryptionKey();
    }

    public static synchronized LspEncryptionManager getInstance() throws NoSuchAlgorithmException {
        if (instance == null) {
            instance = new LspEncryptionManager();
        }
        return instance;
    }
    
    public String encode(Object data) {
    	return LspJsonWebTokenHandler.encode(lspEncryptionKey.getKey(), data);
    }
    
    public String decode(String jwt) {
    	return LspJsonWebTokenHandler.decode(lspEncryptionKey.getKey(), jwt);
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
        
        if (serverStdin != null) {
            serverStdin.write((message + "\n").getBytes());
            serverStdin.flush();
        } else {
            throw new IllegalStateException("Server stdin is not available.");
        }
    }
}
