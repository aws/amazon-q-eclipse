import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginIdcParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;
import software.aws.toolkits.eclipse.amazonq.util.Constants;

import static org.junit.jupiter.api.Assertions.*;

class AuthUtilTest {

    @Test
    void getIssuerUrl_WithBuilderIdLoginType_ReturnsBuilderIdUrl() {
        LoginType loginType = LoginType.BUILDER_ID;
        LoginParams loginParams = null; // Not needed for BUILDER_ID

        String issuerUrl = AuthUtil.getIssuerUrl(loginType, loginParams);

        assertEquals(Constants.AWS_BUILDER_ID_URL, issuerUrl);
    }

    @Test
    void getIssuerUrl_WithIdcLoginType_ReturnsLoginParamsUrl() {
        LoginType loginType = LoginType.IAM_IDENTITY_CENTER;
        LoginParams loginParams = new LoginParams();
        LoginIdcParams idcParams = new LoginIdcParams();
        idcParams.setUrl("https://example.com");
        loginParams.setLoginIdcParams(idcParams);

        String issuerUrl = AuthUtil.getIssuerUrl(loginType, loginParams);

        assertEquals("https://example.com", issuerUrl);
    }
    
    @Test
    void getIssuerUrl_WithNoneLoginType_ReturnsNull() {
        LoginType loginType = LoginType.NONE;
        LoginParams loginParams = new LoginParams();

        String issuerUrl = AuthUtil.getIssuerUrl(loginType, loginParams);

        assertEquals(null, issuerUrl);
    }
    
    @Test
    void getIssuerUrl_WithNull_ReturnsNull() {
        LoginType loginType = null;
        LoginParams loginParams = null;

        String issuerUrl = AuthUtil.getIssuerUrl(loginType, loginParams);

        assertEquals(null, issuerUrl);
    }
    
}
