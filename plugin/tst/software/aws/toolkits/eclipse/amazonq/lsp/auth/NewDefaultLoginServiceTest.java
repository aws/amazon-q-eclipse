// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.auth;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.jsonrpc.messages.ResponseMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import software.aws.toolkits.eclipse.amazonq.configuration.DefaultPluginStore;
import software.aws.toolkits.eclipse.amazonq.configuration.PluginStore;
import software.aws.toolkits.eclipse.amazonq.lsp.AmazonQLspServer;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.GetSsoTokenParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.GetSsoTokenResult;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginIdcParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.Profile;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.SsoSession;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.SsoToken;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.UpdateProfileOptions;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.UpdateProfileParams;
import software.aws.toolkits.eclipse.amazonq.lsp.encryption.LspEncryptionManager;
import software.aws.toolkits.eclipse.amazonq.lsp.model.UpdateCredentialsPayload;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.providers.LspProvider;
import software.aws.toolkits.eclipse.amazonq.util.Constants;
import software.aws.toolkits.eclipse.amazonq.util.LoggingService;
import software.aws.toolkits.eclipse.amazonq.util.QConstants;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class NewDefaultLoginServiceTest {

    private static DefaultLoginService loginService;
    private static LspProvider mockLspProvider;
    private static LspEncryptionManager mockEncryptionManager;
    private static AmazonQLspServer mockAmazonQServer;
    private static PluginStore mockPluginStore;
    private static AuthStateManager mockAuthStateManager;
    private static GetSsoTokenResult mockSsoTokenResult;
    private static MockedStatic<Activator> mockedActivator;
    private static LoggingService mockLoggingService;

    @BeforeEach
    public final void setUp() {
        mockLspProvider = mock(LspProvider.class);
        mockAmazonQServer = mock(AmazonQLspServer.class);
        mockEncryptionManager = mock(LspEncryptionManager.class);
        mockPluginStore = mock(DefaultPluginStore.class);
        mockAuthStateManager = mock(DefaultAuthStateManager.class);
        mockSsoTokenResult = mock(GetSsoTokenResult.class);
        mockLoggingService = mock(LoggingService.class);
        mockedActivator = mockStatic(Activator.class);
        mockedActivator.when(Activator::getLogger).thenReturn(mockLoggingService);

        resetLoginService();
        
        when(mockLspProvider.getAmazonQServer())
        .thenReturn(CompletableFuture.completedFuture(mockAmazonQServer));
        when(mockAmazonQServer.getSsoToken(any()))
            .thenReturn(CompletableFuture.completedFuture(mockSsoTokenResult));
    }
    
    @AfterEach
    void tearDown() throws Exception {
        mockedActivator.close();
    }

    @Test
    void processLogin_BuilderId_NoLoginOnInvalidToken_Success() throws Exception {
        ArgumentCaptor<SsoToken> ssoTokenCaptor = ArgumentCaptor.forClass(SsoToken.class);
        LoginType loginType = LoginType.BUILDER_ID;
        LoginParams loginParams = createValidLoginParams();
        boolean loginOnInvalidToken = false;
        SsoToken expectedSsoToken = createSsoToken();
        
        when(mockSsoTokenResult.ssoToken()).thenReturn(expectedSsoToken);
        when(mockEncryptionManager.decrypt(expectedSsoToken.accessToken())).thenReturn("-decryptedAccessToken-");
        when(mockAmazonQServer.updateTokenCredentials(any())).thenReturn(CompletableFuture.completedFuture(new ResponseMessage()));
        
        invokeProcessLogin(loginType, loginParams, loginOnInvalidToken);
        
        verify(loginService).validateLoginParameters(loginType, loginParams);
        verify(loginService).getToken(loginType, loginParams, loginOnInvalidToken);
        verify(loginService).updateCredentials(ssoTokenCaptor.capture());
        String ssoTokenId = ssoTokenCaptor.getValue().id();
        assertEquals(expectedSsoToken.id(), ssoTokenId);
        verify(mockAuthStateManager).toLoggedIn(loginType, loginParams, ssoTokenId);
        verify(mockLoggingService).info("Successfully logged in");
    }
    
    @Test
    void processLogin_BuilderId_WithLoginOnInvalidToken_Success() throws Exception {
        ArgumentCaptor<SsoToken> ssoTokenCaptor = ArgumentCaptor.forClass(SsoToken.class);
        LoginType loginType = LoginType.BUILDER_ID;
        LoginParams loginParams = createValidLoginParams();
        boolean loginOnInvalidToken = true;
        SsoToken expectedSsoToken = createSsoToken();
        
        when(mockSsoTokenResult.ssoToken()).thenReturn(expectedSsoToken);
        when(mockEncryptionManager.decrypt(expectedSsoToken.accessToken())).thenReturn("-decryptedAccessToken-");
        when(mockAmazonQServer.updateTokenCredentials(any())).thenReturn(CompletableFuture.completedFuture(new ResponseMessage()));
        
        invokeProcessLogin(loginType, loginParams, loginOnInvalidToken);
        
        verify(loginService).validateLoginParameters(loginType, loginParams);
        verify(loginService).getToken(loginType, loginParams, loginOnInvalidToken);
        verify(loginService).updateCredentials(ssoTokenCaptor.capture());
        String ssoTokenId = ssoTokenCaptor.getValue().id();
        assertEquals(expectedSsoToken.id(), ssoTokenId);
        verify(mockAuthStateManager).toLoggedIn(loginType, loginParams, ssoTokenId);
        verify(mockLoggingService).info("Successfully logged in");
    }
    
    @Test
    void processLogin_IDC_NoLoginOnInvalidToken_Success() throws Exception {
        ArgumentCaptor<SsoToken> ssoTokenCaptor = ArgumentCaptor.forClass(SsoToken.class);
        LoginType loginType = LoginType.IAM_IDENTITY_CENTER;
        LoginParams loginParams = createValidLoginParams();
        boolean loginOnInvalidToken = false;
        SsoToken expectedSsoToken = createSsoToken();
        
        when(mockSsoTokenResult.ssoToken()).thenReturn(expectedSsoToken);
        when(mockEncryptionManager.decrypt(expectedSsoToken.accessToken())).thenReturn("-decryptedAccessToken-");
        when(mockAmazonQServer.updateTokenCredentials(any())).thenReturn(CompletableFuture.completedFuture(new ResponseMessage()));
        
        invokeProcessLogin(loginType, loginParams, loginOnInvalidToken);
        
        verify(loginService).validateLoginParameters(loginType, loginParams);
        verify(loginService).getToken(loginType, loginParams, loginOnInvalidToken);
        verify(loginService).updateCredentials(ssoTokenCaptor.capture());
        String ssoTokenId = ssoTokenCaptor.getValue().id();
        assertEquals(expectedSsoToken.id(), ssoTokenId);
        verify(mockAuthStateManager).toLoggedIn(loginType, loginParams, ssoTokenId);
        verify(mockLoggingService).info("Successfully logged in");
    }
    
    @Test
    void processLogin_IDC_WithLoginOnInvalidToken_Success() throws Exception {
        ArgumentCaptor<UpdateProfileParams> updateProfileParamsCaptor = ArgumentCaptor.forClass(UpdateProfileParams.class);
        ArgumentCaptor<SsoToken> ssoTokenCaptor = ArgumentCaptor.forClass(SsoToken.class);
        LoginType loginType = LoginType.IAM_IDENTITY_CENTER;
        LoginParams loginParams = createValidLoginParams();
        boolean loginOnInvalidToken = true;
        SsoToken expectedSsoToken = createSsoToken();
        
        when(mockSsoTokenResult.ssoToken()).thenReturn(expectedSsoToken);
        when(mockEncryptionManager.decrypt(expectedSsoToken.accessToken())).thenReturn("-decryptedAccessToken-");
        when(mockAmazonQServer.updateTokenCredentials(any())).thenReturn(CompletableFuture.completedFuture(new ResponseMessage()));
        when(mockAmazonQServer.updateProfile(any()))
        .thenReturn(CompletableFuture.completedFuture(null));
        
        invokeProcessLogin(loginType, loginParams, loginOnInvalidToken);
        
        verify(loginService).validateLoginParameters(loginType, loginParams);
        verify(loginService).getToken(loginType, loginParams, loginOnInvalidToken);
        verify(loginService).updateCredentials(ssoTokenCaptor.capture());
        String ssoTokenId = ssoTokenCaptor.getValue().id();
        assertEquals(expectedSsoToken.id(), ssoTokenId);
        verify(mockAmazonQServer).updateProfile(updateProfileParamsCaptor.capture());
        UpdateProfileParams actualParams = updateProfileParamsCaptor.getValue();
        verifyUpdateProfileParams(actualParams);
        verify(mockAuthStateManager).toLoggedIn(loginType, loginParams, ssoTokenId);
        verify(mockLoggingService).info("Successfully logged in");
    }

    @Test
    void getToken_BuilderId_NoLoginOnInvalidToken_Success() throws Exception {
        LoginType loginType = LoginType.BUILDER_ID;
        LoginParams loginParams = new LoginParams(); // LoginParams is not required for BUILDER_ID
        SsoToken expectedToken = createSsoToken();
        when(mockSsoTokenResult.ssoToken()).thenReturn(expectedToken);
        boolean loginOnInvalidToken = false;

        SsoToken actualToken = invokeGetToken(loginType, loginParams, loginOnInvalidToken);
        
        assertEquals(expectedToken.id(), actualToken.id());
        assertEquals(expectedToken.accessToken(), actualToken.accessToken());
        verify(mockAmazonQServer).getSsoToken(any(GetSsoTokenParams.class));
        verifyNoMoreInteractions(mockAmazonQServer);
    }
    
    @Test
    void getToken_BuilderId_WithLoginOnInvalidToken_Success() throws Exception {
        LoginType loginType = LoginType.BUILDER_ID;
        LoginParams loginParams = new LoginParams(); // LoginParams is not required for BUILDER_ID
        SsoToken expectedToken = createSsoToken();
        when(mockSsoTokenResult.ssoToken()).thenReturn(expectedToken);
        boolean loginOnInvalidToken = true;

        SsoToken actualToken = invokeGetToken(loginType, loginParams, loginOnInvalidToken);
        
        assertEquals(expectedToken.id(), actualToken.id());
        assertEquals(expectedToken.accessToken(), actualToken.accessToken());
        verify(mockAmazonQServer).getSsoToken(any(GetSsoTokenParams.class));
        verifyNoMoreInteractions(mockAmazonQServer);
    }
    
    @Test
    void getToken_IDC_NoLoginOnInvalidToken_Success() throws Exception {
        LoginType loginType = LoginType.IAM_IDENTITY_CENTER;
        LoginParams loginParams = createValidLoginParams();
        SsoToken expectedToken = createSsoToken();
        when(mockSsoTokenResult.ssoToken()).thenReturn(expectedToken);
        boolean loginOnInvalidToken = false;

        SsoToken actualToken = invokeGetToken(loginType, loginParams, loginOnInvalidToken);
        
        assertEquals(expectedToken.id(), actualToken.id());
        assertEquals(expectedToken.accessToken(), actualToken.accessToken());
        verify(mockAmazonQServer).getSsoToken(any(GetSsoTokenParams.class));
        verifyNoMoreInteractions(mockAmazonQServer);
    }
    
    @Test
    void getToken_IDC_WithLoginOnInvalidToken_Success() throws Exception {
        ArgumentCaptor<UpdateProfileParams> updateProfileParamsCaptor = ArgumentCaptor.forClass(UpdateProfileParams.class);
        when(mockAmazonQServer.updateProfile(any()))
            .thenReturn(CompletableFuture.completedFuture(null));
        
        LoginType loginType = LoginType.IAM_IDENTITY_CENTER;
        LoginParams loginParams = createValidLoginParams();
        SsoToken expectedToken = createSsoToken();
        when(mockSsoTokenResult.ssoToken()).thenReturn(expectedToken);
        boolean loginOnInvalidToken = true;

        SsoToken actualToken = invokeGetToken(loginType, loginParams, loginOnInvalidToken);
        
        assertEquals(expectedToken.id(), actualToken.id());
        assertEquals(expectedToken.accessToken(), actualToken.accessToken());
        verify(mockAmazonQServer).updateProfile(updateProfileParamsCaptor.capture());
        UpdateProfileParams actualParams = updateProfileParamsCaptor.getValue();
        verifyUpdateProfileParams(actualParams);
        verify(mockAmazonQServer).getSsoToken(any(GetSsoTokenParams.class));
        verifyNoMoreInteractions(mockAmazonQServer);
    }
    
    @Test 
    void validateLoginParameters_WithNullLoginType_ThrowsException() {
        LoginType loginType = null;
        LoginParams loginParams = createValidLoginParams();
        
        try {
            loginService.validateLoginParameters(loginType, loginParams);
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
            loginService.validateLoginParameters(loginType, loginParams);
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
            loginService.validateLoginParameters(loginType, loginParams);
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
            loginService.validateLoginParameters(loginType, loginParams);
        } catch (Exception ex) {
            fail("Expected no exception");
        }
    }
    
    private void resetLoginService() {
      loginService = new DefaultLoginService.Builder()
              .withLspProvider(mockLspProvider)
              .withPluginStore(mockPluginStore)
              .withEncryptionManager(mockEncryptionManager)
              .withAuthStateManager(mockAuthStateManager)
              .build();
      loginService = spy(loginService);
    }

    private LoginParams createValidLoginParams() {
        LoginParams loginParams = new LoginParams();
        LoginIdcParams idcParams = new LoginIdcParams();
        idcParams.setRegion("test-region");
        idcParams.setUrl("https://example.com");
        loginParams.setLoginIdcParams(idcParams);
        return loginParams;
    }
    
    private SsoToken createSsoToken() {
        String id = "ssoTokenId";
        String accessToken = "ssoAccessToken";
        return new SsoToken(id, accessToken);
    }
    
    private boolean verifyUpdateProfileParams(final UpdateProfileParams params) {
      Profile profile = params.profile();
      SsoSession ssoSession = params.ssoSession();
      UpdateProfileOptions options = params.options();
    
      return profile.getName().equals(Constants.IDC_PROFILE_NAME)
              && profile.getProfileKinds().equals(Collections.singletonList(Constants.IDC_PROFILE_KIND))
              && profile.getProfileSettings().region().equals("testRegion")
              && profile.getProfileSettings().ssoSession().equals(Constants.IDC_SESSION_NAME)
              && ssoSession.getName().equals(Constants.IDC_SESSION_NAME)
              && ssoSession.getSsoSessionSettings().ssoStartUrl().equals("testUrl")
              && ssoSession.getSsoSessionSettings().ssoRegion().equals("testRegion")
              && ssoSession.getSsoSessionSettings().ssoRegistrationScopes() == QConstants.Q_SCOPES
              && options.createNonexistentProfile()
              && options.createNonexistentSsoSession()
              && options.ensureSsoAccountAccessScope()
              && !options.updateSharedSsoSession();
    }

    private SsoToken invokeGetToken(LoginType loginType, LoginParams loginParams, boolean loginOnInvalidToken) throws Exception {
        Object getTokenFuture = loginService.getToken(loginType, loginParams, loginOnInvalidToken);
        assertTrue(getTokenFuture instanceof CompletableFuture<?>, "Return value should be CompletableFuture");

        CompletableFuture<?> future = (CompletableFuture<?>) getTokenFuture;
        Object result = future.get();
        assertTrue(result instanceof SsoToken, "getTokenFuture result should be SsoToken");
        
        return (SsoToken) result;
    }
    

    private void invokeProcessLogin(LoginType loginType, LoginParams loginParams, boolean loginOnInvalidToken) throws Exception {
        Object processLoginFuture = loginService.processLogin(loginType, loginParams, loginOnInvalidToken);
        assertTrue(processLoginFuture instanceof CompletableFuture<?>, "Return value should be CompletableFuture");

        CompletableFuture<?> future = (CompletableFuture<?>) processLoginFuture;
        Object result = future.get();
        assertNull(result);
    }
}
