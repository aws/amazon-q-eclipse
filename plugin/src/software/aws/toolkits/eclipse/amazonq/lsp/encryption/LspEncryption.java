package software.aws.toolkits.eclipse.amazonq.lsp.encryption;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;

public class LspEncryption {
    
    private static LspEncryption instance;
    private String key;
    
    private LspEncryption() {
        key = generateRandomKey();
    }
    
    public static synchronized LspEncryption getInstance() {
        if (instance == null) {
            instance = new LspEncryption();
        } 
        return instance;
    }
    
    public String encrypt(String message) {
        // TODO
        return "";
    }
    
    public String decrypt(String encryptedMessage) {
        // TODO
        return "";
    }
    
    public void initializeEncrypedCommunication(OutputStream serverStdin) throws IOException {
        String message = String.format("{\"version\": \"1.0\",\"key\":\"%s\",\"mode\":\"JWT\"}", base64Encode(key));
        sendMessageToServer(serverStdin, message);
    }

    private String base64Encode(String str) {
        byte[] encodedBytes = Base64.getEncoder().encode(str.getBytes(StandardCharsets.UTF_8));
        return new String(encodedBytes, StandardCharsets.UTF_8);
    }

    private String generateRandomKey() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[128]; // 128 bytes = 256 hex characters
        secureRandom.nextBytes(randomBytes);

        return HexFormat.of().formatHex(randomBytes);
    }
    
    private void sendMessageToServer(OutputStream serverStdin, String message) throws IOException {
        if (serverStdin != null) {
            PluginLogger.info("Sending message: " + message);
            serverStdin.write((message + "\n").getBytes());
            serverStdin.flush();
        } else {
            throw new IllegalStateException("Server stdin is not available. Did you start the server?");
        }
    }
}
