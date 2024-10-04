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

public final class LspJsonWebTokenHandler {
	
	public static String encode(final SecretKey encryptionKey, final Object data) {
//        Integer minute = 60 * 1000;
//        Long currentTime = System.currentTimeMillis();
//        
//        Long notBefore = currentTime - minute;
//        Long expiresOn = currentTime + minute;
		
		try {
	        JsonHandler jsonHandler = new JsonHandler();
			
	        JWEHeader header = new JWEHeader(JWEAlgorithm.DIR, EncryptionMethod.A256GCM);
	        Payload payload = new Payload(jsonHandler.serialize(data));
	        JWEObject jweObject = new JWEObject(header, payload);
	        
	        jweObject.encrypt(new DirectEncrypter(encryptionKey));
	
	        return jweObject.serialize();
		} catch (Exception e) {
			throw new AmazonQPluginException("Error occurred while encrypting jwt", e);
		}
    }

    public static String decode(final SecretKey encryptionKey, final String jwt) {
    	try {
    		JWEObject jweObject = JWEObject.parse(jwt);

            jweObject.decrypt(new DirectDecrypter(encryptionKey));

	        return jweObject.getPayload().toString();
    	} catch (Exception e) {
    		throw new AmazonQPluginException("Error occurred while decrypting jwt", e);
    	}
    }
}