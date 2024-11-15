package software.aws.toolkits.eclipse.amazonq.lsp.encryption;

import javax.crypto.SecretKey;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;

import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.util.JsonHandler;

public final class LspJsonWebToken {

    private LspJsonWebToken() {
        // prevent instantiation
    }

    public static String encrypt(final SecretKey encryptionKey, final Object data) {
        JsonHandler jsonHandler = new JsonHandler();
        String serializedData = jsonHandler.serialize(data);

        try {
            JWEHeader header = new JWEHeader(JWEAlgorithm.DIR, EncryptionMethod.A256GCM);
            Payload payload = new Payload(serializedData);
            JWEObject jweObject = new JWEObject(header, payload);

            jweObject.encrypt(new DirectEncrypter(encryptionKey));

            return jweObject.serialize();
        } catch (Exception e) {
            throw new AmazonQPluginException("Error occurred while encrypting JWT", e);
        }
    }

    public static String decrypt(final SecretKey encryptionKey, final String jwt) {
        try {
            JWEObject jweObject = JWEObject.parse(jwt);
            jweObject.decrypt(new DirectDecrypter(encryptionKey));

            return jweObject.getPayload().toString();
        } catch (Exception e) {
            throw new AmazonQPluginException("Error occurred while decrypting JWT", e);
        }
    }
}
