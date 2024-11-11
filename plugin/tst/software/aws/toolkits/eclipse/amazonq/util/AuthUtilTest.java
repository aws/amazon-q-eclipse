// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import org.junit.jupiter.api.Test;

import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginIdcParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;
import software.aws.toolkits.eclipse.amazonq.util.AuthUtil;
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
    
    @Test 
    void validateLoginParameters_WithNullLoginType_ThrowsException() {
        LoginType loginType = null;
        LoginParams loginParams = createValidLoginParams();
        
        try {
            AuthUtil.validateLoginParameters(loginType, loginParams);
            fail("Expected IllegalArgumentException to be thrown");
        } catch (Exception ex) {
            assertTrue(ex instanceof IllegalArgumentException);
            assertEquals("Missing required parameter: loginType cannot be null", 
                ex.getMessage());
        }
    }
    
    @Test 
    void validateLoginParameters_WithNoneLoginType_ThrowsException() {
        LoginType loginType = LoginType.NONE;
        LoginParams loginParams = createValidLoginParams();
        
        try {
            AuthUtil.validateLoginParameters(loginType, loginParams);
            fail("Expected IllegalArgumentException to be thrown");
        } catch (Exception ex) {
            assertTrue(ex instanceof IllegalArgumentException);
            assertEquals("Invalid loginType: NONE is not a valid login type", 
                ex.getMessage());
        }
    }
    
    @Test 
    void validateLoginParameters_WithNullLoginParams_ThrowsException() {
        LoginType loginType = LoginType.BUILDER_ID;
        LoginParams loginParams = null;
        
        try {
            AuthUtil.validateLoginParameters(loginType, loginParams);
            fail("Expected IllegalArgumentException to be thrown");
        } catch (Exception ex) {
            assertTrue(ex instanceof IllegalArgumentException);
            assertEquals("Missing required parameter: loginParams cannot be null", 
                ex.getMessage());
        }
    }
    
    @Test 
    void validateLoginParameters_WithValidParams_Success() {
        LoginType loginType = LoginType.BUILDER_ID;
        LoginParams loginParams = createValidLoginParams();
        
        try {
            AuthUtil.validateLoginParameters(loginType, loginParams);
        } catch (Exception ex) {
            fail("Expected no exception");
        }
    }
    
    private LoginParams createValidLoginParams() {
        LoginParams loginParams = new LoginParams();
        LoginIdcParams idcParams = new LoginIdcParams();
        idcParams.setRegion("test-region");
        idcParams.setUrl("https://example.com");
        loginParams.setLoginIdcParams(idcParams);
        return loginParams;
    }
}
