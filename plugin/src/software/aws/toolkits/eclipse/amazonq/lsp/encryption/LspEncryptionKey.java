package software.aws.toolkits.eclipse.amazonq.lsp.encryption;

import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class LspEncryptionKey {
    private SecretKey key;

    public LspEncryptionKey() throws NoSuchAlgorithmException {
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
    
    public static SecretKey generateKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        return keyGen.generateKey();
    }
}
