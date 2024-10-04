package software.aws.toolkits.eclipse.amazonq.lsp.encryption;

import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.AESDecrypter;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.util.JsonHandler;

public final class LspJsonWebToken {
	
	private SecretKey encryptionKey;
	
	public LspJsonWebToken(SecretKey encryptionKey) {
		this.encryptionKey = encryptionKey;
	}
	
	public String encode(final Object data) throws KeyLengthException, JOSEException {
//        Integer minute = 60 * 1000;
//        Long currentTime = System.currentTimeMillis();
//        
//        Long notBefore = currentTime - minute;
//        Long expiresOn = currentTime + minute;
		
        JsonHandler jsonHandler = new JsonHandler();
		
        JWEHeader header = new JWEHeader(JWEAlgorithm.DIR, EncryptionMethod.A256GCM);
        Payload payload = new Payload(jsonHandler.serialize(data));
        JWEObject jweObject = new JWEObject(header, payload);
        
        jweObject.encrypt(new DirectEncrypter(encryptionKey));

        return jweObject.serialize();
    }

    public String decode(final String jweString) {
    	try {
    		JWEObject jweObject = JWEObject.parse(jweString);

            jweObject.decrypt(new DirectDecrypter(encryptionKey));

            String jwePayload = jweObject.getPayload().toString();
            
	        return jwePayload;
    	} catch (Exception e) {
    		throw new AmazonQPluginException("Error occurred: Attempting to decode an expired Lsp JWT");
    	}
    }
}