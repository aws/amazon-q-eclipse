package software.aws.toolkits.eclipse.amazonq.lsp.encryption;

import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class LspEncryptionKey {
    private String base64encodedKey;

    public LspEncryptionKey() throws NoSuchAlgorithmException {
        this.base64encodedKey = base64Encode(generateKey());
    }

    public String getKey() {
        return base64encodedKey;
    }

    private String base64Encode(final SecretKey key) {
    	String encodedKey = Base64.getEncoder().encodeToString(key.getEncoded());
    	return encodedKey;
    }
    
    public static SecretKey generateKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
        keyGen.init(256); // 256-bit key for HS256
        return keyGen.generateKey();
    }
}
