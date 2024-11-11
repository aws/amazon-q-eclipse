// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.auth;

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
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.GetSsoTokenResult;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginIdcParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.SsoToken;
import software.aws.toolkits.eclipse.amazonq.lsp.encryption.LspEncryptionManager;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.providers.LspProvider;
import software.aws.toolkits.eclipse.amazonq.util.AuthUtil;
import software.aws.toolkits.eclipse.amazonq.util.LoggingService;

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
    private static MockedStatic<AuthUtil> mockedAuthUtil;
    private static LoggingService mockLoggingService;
    private static AuthTokenService mockedAuthTokenService;
    private static AuthCredentialsService mockedAuthCredentialsService;

    @BeforeEach
    public final void setUp() {
        mockLspProvider = mock(LspProvider.class);
        mockAmazonQServer = mock(AmazonQLspServer.class);
        mockEncryptionManager = mock(LspEncryptionManager.class);
        mockPluginStore = mock(DefaultPluginStore.class);
        mockAuthStateManager = mock(DefaultAuthStateManager.class);
        mockSsoTokenResult = mock(GetSsoTokenResult.class);
        mockLoggingService = mock(LoggingService.class);
        mockedAuthTokenService = mock(AuthTokenService.class);
        mockedAuthCredentialsService = mock(AuthCredentialsService.class);
        mockedActivator = mockStatic(Activator.class);
        mockedActivator.when(Activator::getLogger).thenReturn(mockLoggingService);
        mockedAuthUtil = mockStatic(AuthUtil.class);

        resetLoginService();
        
        when(mockLspProvider.getAmazonQServer())
        .thenReturn(CompletableFuture.completedFuture(mockAmazonQServer));
        when(mockAmazonQServer.getSsoToken(any()))
            .thenReturn(CompletableFuture.completedFuture(mockSsoTokenResult));
    }
    
    @AfterEach
    void tearDown() throws Exception {
        mockedActivator.close();
        mockedAuthUtil.close();
    }

    @Test
    void processLogin_BuilderId_NoLoginOnInvalidToken_Success() throws Exception {
        LoginType loginType = LoginType.BUILDER_ID;
        LoginParams loginParams = createValidLoginParams();
        boolean loginOnInvalidToken = false;
        SsoToken expectedSsoToken = createSsoToken();
        
        when(mockSsoTokenResult.ssoToken()).thenReturn(expectedSsoToken);
        when(mockEncryptionManager.decrypt(expectedSsoToken.accessToken())).thenReturn("-decryptedAccessToken-");
        when(mockedAuthTokenService.getSsoToken(loginType, loginParams, loginOnInvalidToken)).thenReturn(CompletableFuture.completedFuture(expectedSsoToken));
        when(mockedAuthCredentialsService.updateTokenCredentials(expectedSsoToken.accessToken(), true)).thenReturn(CompletableFuture.completedFuture(new ResponseMessage()));
        
        invokeProcessLogin(loginType, loginParams, loginOnInvalidToken);
        
        mockedAuthUtil.verify(() -> AuthUtil.validateLoginParameters(loginType, loginParams));
        verify(mockedAuthTokenService).getSsoToken(loginType, loginParams, loginOnInvalidToken);
        verify(mockedAuthCredentialsService).updateTokenCredentials(expectedSsoToken.accessToken(), true);
        verify(mockAuthStateManager).toLoggedIn(loginType, loginParams, expectedSsoToken.id());
        verify(mockLoggingService).info("Successfully logged in");
    }
    
    @Test
    void processLogin_BuilderId_WithLoginOnInvalidToken_Success() throws Exception {
        LoginType loginType = LoginType.BUILDER_ID;
        LoginParams loginParams = createValidLoginParams();
        boolean loginOnInvalidToken = true;
        SsoToken expectedSsoToken = createSsoToken();
        
        when(mockSsoTokenResult.ssoToken()).thenReturn(expectedSsoToken);
        when(mockEncryptionManager.decrypt(expectedSsoToken.accessToken())).thenReturn("-decryptedAccessToken-");
        when(mockedAuthTokenService.getSsoToken(loginType, loginParams, loginOnInvalidToken)).thenReturn(CompletableFuture.completedFuture(expectedSsoToken));
        when(mockedAuthCredentialsService.updateTokenCredentials(expectedSsoToken.accessToken(), true)).thenReturn(CompletableFuture.completedFuture(new ResponseMessage()));
        
        invokeProcessLogin(loginType, loginParams, loginOnInvalidToken);
        
        mockedAuthUtil.verify(() -> AuthUtil.validateLoginParameters(loginType, loginParams));
        verify(mockedAuthTokenService).getSsoToken(loginType, loginParams, loginOnInvalidToken);
        verify(mockedAuthCredentialsService).updateTokenCredentials(expectedSsoToken.accessToken(), true);
        verify(mockAuthStateManager).toLoggedIn(loginType, loginParams, expectedSsoToken.id());
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
        when(mockedAuthTokenService.getSsoToken(loginType, loginParams, loginOnInvalidToken)).thenReturn(CompletableFuture.completedFuture(expectedSsoToken));
        when(mockedAuthCredentialsService.updateTokenCredentials(expectedSsoToken.accessToken(), true)).thenReturn(CompletableFuture.completedFuture(new ResponseMessage()));
        
        invokeProcessLogin(loginType, loginParams, loginOnInvalidToken);
        
        mockedAuthUtil.verify(() -> AuthUtil.validateLoginParameters(loginType, loginParams));
        verify(mockedAuthTokenService).getSsoToken(loginType, loginParams, loginOnInvalidToken);
        verify(mockedAuthCredentialsService).updateTokenCredentials(expectedSsoToken.accessToken(), true);
        verify(mockAuthStateManager).toLoggedIn(loginType, loginParams, expectedSsoToken.id());
        verify(mockLoggingService).info("Successfully logged in");
    }
    
    @Test
    void processLogin_IDC_WithLoginOnInvalidToken_Success() throws Exception {
        LoginType loginType = LoginType.IAM_IDENTITY_CENTER;
        LoginParams loginParams = createValidLoginParams();
        boolean loginOnInvalidToken = true;
        SsoToken expectedSsoToken = createSsoToken();
        
        when(mockSsoTokenResult.ssoToken()).thenReturn(expectedSsoToken);
        when(mockEncryptionManager.decrypt(expectedSsoToken.accessToken())).thenReturn("-decryptedAccessToken-");
        when(mockedAuthTokenService.getSsoToken(loginType, loginParams, loginOnInvalidToken)).thenReturn(CompletableFuture.completedFuture(expectedSsoToken));
        when(mockedAuthCredentialsService.updateTokenCredentials(expectedSsoToken.accessToken(), true)).thenReturn(CompletableFuture.completedFuture(new ResponseMessage()));
        
        invokeProcessLogin(loginType, loginParams, loginOnInvalidToken);
        
        mockedAuthUtil.verify(() -> AuthUtil.validateLoginParameters(loginType, loginParams));
        verify(mockedAuthTokenService).getSsoToken(loginType, loginParams, loginOnInvalidToken);
        verify(mockedAuthCredentialsService).updateTokenCredentials(expectedSsoToken.accessToken(), true);
        verify(mockAuthStateManager).toLoggedIn(loginType, loginParams, expectedSsoToken.id());
        verify(mockLoggingService).info("Successfully logged in");
    }

    private void resetLoginService() {
      loginService = new DefaultLoginService.Builder()
              .withLspProvider(mockLspProvider)
              .withPluginStore(mockPluginStore)
              .withEncryptionManager(mockEncryptionManager)
              .withAuthStateManager(mockAuthStateManager)
              .withAuthCredentialsService(mockedAuthCredentialsService)
              .withAuthTokenService(mockedAuthTokenService)
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

    private void invokeProcessLogin(LoginType loginType, LoginParams loginParams, boolean loginOnInvalidToken) throws Exception {
        Object processLoginFuture = loginService.processLogin(loginType, loginParams, loginOnInvalidToken);
        assertTrue(processLoginFuture instanceof CompletableFuture<?>, "Return value should be CompletableFuture");

        CompletableFuture<?> future = (CompletableFuture<?>) processLoginFuture;
        Object result = future.get();
        assertNull(result);
    }
}
