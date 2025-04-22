// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.auth;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import software.aws.toolkits.eclipse.amazonq.configuration.DefaultPluginStore;
import software.aws.toolkits.eclipse.amazonq.configuration.PluginStore;
import software.aws.toolkits.eclipse.amazonq.configuration.customization.CustomizationUtil;
import software.aws.toolkits.eclipse.amazonq.lsp.AmazonQLspServer;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthStateType;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.GetSsoTokenResult;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginIdcParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.SsoToken;
import software.aws.toolkits.eclipse.amazonq.lsp.model.ConnectionMetadata;
import software.aws.toolkits.eclipse.amazonq.lsp.model.UpdateCredentialsPayload;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.providers.lsp.LspProvider;
import software.aws.toolkits.eclipse.amazonq.util.AuthUtil;
import software.aws.toolkits.eclipse.amazonq.util.Constants;
import software.aws.toolkits.eclipse.amazonq.util.LoggingService;

public final class DefaultLoginServiceTest {

    private static DefaultLoginService loginService;
    private static LspProvider mockLspProvider;
    private static AmazonQLspServer mockAmazonQServer;
    private static PluginStore mockPluginStore;
    private static AuthStateManager mockAuthStateManager;
    private static MockedStatic<Activator> mockedActivator;
    private static MockedStatic<AuthUtil> mockedAuthUtil;
    private static LoggingService mockLoggingService;
    private static AuthTokenService mockedAuthTokenService;
    private static AuthCredentialsService mockedAuthCredentialsService;
    private static UpdateCredentialsPayload updateCredentialsPayload;
    private static GetSsoTokenResult expectedSsoToken;
    private static SsoToken ssoToken;
    private static MockedStatic<CustomizationUtil> mockedCustomizationUtil;

    @BeforeEach
    public void setUp() {
        mockLspProvider = mock(LspProvider.class);
        mockAmazonQServer = mock(AmazonQLspServer.class);
        mockPluginStore = mock(DefaultPluginStore.class);
        mockAuthStateManager = mock(DefaultAuthStateManager.class);
        mockLoggingService = mock(LoggingService.class);
        mockedAuthTokenService = mock(AuthTokenService.class);
        mockedAuthCredentialsService = mock(AuthCredentialsService.class);
        mockedActivator = mockStatic(Activator.class);
        mockedActivator.when(Activator::getLogger).thenReturn(mockLoggingService);
        mockedAuthUtil = mockStatic(AuthUtil.class);
        mockedActivator.when(Activator::getLspProvider).thenReturn(mockLspProvider);
        mockedCustomizationUtil = mockStatic(CustomizationUtil.class);

        updateCredentialsPayload = mock(UpdateCredentialsPayload.class);
        when(updateCredentialsPayload.data()).thenReturn("data");
        when(updateCredentialsPayload.metadata()).thenReturn(new ConnectionMetadata());
        when(updateCredentialsPayload.encrypted()).thenReturn(true);

        ssoToken = mock(SsoToken.class);
        when(ssoToken.id()).thenReturn("ssoTokenId");
        when(ssoToken.accessToken()).thenReturn("ssoAccessToken");

        expectedSsoToken = mock(GetSsoTokenResult.class);
        when(expectedSsoToken.getUpdateCredentialsPayloadHydratedWithStartUrl(any(String.class)))
                .thenReturn(updateCredentialsPayload);
        when(expectedSsoToken.ssoToken()).thenReturn(ssoToken);
        when(expectedSsoToken.updateCredentialsParams()).thenReturn(updateCredentialsPayload);

        resetLoginService();

        when(mockLspProvider.getAmazonQServer()).thenReturn(CompletableFuture.completedFuture(mockAmazonQServer));
        when(mockAmazonQServer.getSsoToken(any())).thenReturn(CompletableFuture.completedFuture(expectedSsoToken));
    }

    @AfterEach
    void tearDown() throws Exception {
        mockedActivator.close();
        mockedAuthUtil.close();
        mockedCustomizationUtil.close();
    }

    @Test
    void loginWhenAlreadyLoggedInValidation() {
        LoginType loginType = LoginType.BUILDER_ID;
        LoginIdcParams idcParams = createLoginIdcParams("test-region", "test-url");
        LoginParams loginParams = createLoginParams(idcParams);
        AuthState authState = createAuthState(AuthStateType.LOGGED_IN, LoginType.BUILDER_ID, loginParams,
                Constants.AWS_BUILDER_ID_URL, "test-sso-token-id");

        when(mockAuthStateManager.getAuthState()).thenReturn(authState);

        CompletableFuture<Void> result = loginService.login(loginType, loginParams);

        assertTrue(result.isDone());
        verify(mockLoggingService).warn("Attempted to log in while already in a logged in state");
        verifyNoMoreInteractions(mockedAuthTokenService, mockedAuthCredentialsService);
    }

    @Test
    void loginBuilderIdSuccess() {
        LoginType loginType = LoginType.BUILDER_ID;
        LoginIdcParams idcParams = createLoginIdcParams("test-region", "test-url");
        LoginParams loginParams = createLoginParams(idcParams);
        AuthState authState = createAuthState(AuthStateType.LOGGED_OUT, LoginType.NONE, null, null, null);

        when(mockAuthStateManager.getAuthState()).thenReturn(authState);
        when(mockedAuthTokenService.getSsoToken(loginType, loginParams, true))
                .thenReturn(CompletableFuture.completedFuture(expectedSsoToken));
        when(mockedAuthCredentialsService.updateTokenCredentials(updateCredentialsPayload))
                .thenReturn(CompletableFuture.completedFuture(null));

        CompletableFuture<Void> result = loginService.login(loginType, loginParams);

        assertNotNull(result);
        verify(mockLoggingService).info("Attempting to login...");
        verify(mockLoggingService).info("Successfully logged in");
        verify(mockedAuthTokenService).getSsoToken(loginType, loginParams, true);
        verify(mockedAuthCredentialsService).updateTokenCredentials(updateCredentialsPayload);
        verifyNoMoreInteractions(mockedAuthTokenService, mockedAuthCredentialsService);
    }

    @Test
    void loginIdcSuccess() {
        LoginType loginType = LoginType.IAM_IDENTITY_CENTER;
        LoginIdcParams idcParams = createLoginIdcParams("test-region", "test-url");
        LoginParams loginParams = createLoginParams(idcParams);
        AuthState authState = createAuthState(AuthStateType.LOGGED_OUT, LoginType.NONE, null, null, null);

        when(mockAuthStateManager.getAuthState()).thenReturn(authState);
        when(mockedAuthTokenService.getSsoToken(loginType, loginParams, true))
                .thenReturn(CompletableFuture.completedFuture(expectedSsoToken));
        when(mockedAuthCredentialsService.updateTokenCredentials(updateCredentialsPayload))
                .thenReturn(CompletableFuture.completedFuture(null));

        CompletableFuture<Void> result = loginService.login(loginType, loginParams);

        assertNotNull(result);
        verify(mockLoggingService).info("Attempting to login...");
        verify(mockLoggingService).info("Successfully logged in");
        verify(mockedAuthTokenService).getSsoToken(loginType, loginParams, true);
        verify(mockedAuthCredentialsService).updateTokenCredentials(updateCredentialsPayload);
        verify(expectedSsoToken).getUpdateCredentialsPayloadHydratedWithStartUrl(any(String.class));
        verifyNoMoreInteractions(mockedAuthTokenService, mockedAuthCredentialsService);
    }

    @Test
    void logoutWhenAlreadyLoggedOutValidation() {
        AuthState authState = createAuthState(AuthStateType.LOGGED_OUT, LoginType.NONE, null, null, null);
        when(mockAuthStateManager.getAuthState()).thenReturn(authState);

        CompletableFuture<Void> result = loginService.logout();

        assertTrue(result.isDone());
        verify(mockLoggingService).warn("Attempted to log out while already in a logged out state");
        verifyNoInteractions(mockedAuthTokenService, mockedAuthCredentialsService);
    }

    @Test
    void logoutWithNullSsoTokenIdValidation() {
        AuthState authState = mock(AuthState.class);
        when(mockAuthStateManager.getAuthState()).thenReturn(authState);
        when(authState.ssoTokenId()).thenReturn(null);

        CompletableFuture<Void> result = loginService.logout();

        assertTrue(result.isDone());
        verify(mockAuthStateManager).toLoggedOut();
        verify(mockLoggingService).warn("Attempted to log out with no ssoTokenId saved in auth state");
        verifyNoInteractions(mockedAuthTokenService, mockedAuthCredentialsService);
    }

    @Test
    void logoutWithBlankSsoTokenIdValidation() {
        AuthState authState = mock(AuthState.class);
        when(mockAuthStateManager.getAuthState()).thenReturn(authState);
        when(authState.ssoTokenId()).thenReturn("");

        CompletableFuture<Void> result = loginService.logout();

        assertTrue(result.isDone());
        verify(mockAuthStateManager).toLoggedOut();
        verify(mockLoggingService).warn("Attempted to log out with no ssoTokenId saved in auth state");
        verifyNoInteractions(mockedAuthTokenService, mockedAuthCredentialsService);
    }

    @Test
    void expireSuccess() {
        UpdateCredentialsPayload updateCredentialsPayload = new UpdateCredentialsPayload(null, null, false);
        when(mockedAuthCredentialsService.updateTokenCredentials(updateCredentialsPayload))
                .thenReturn(CompletableFuture.completedFuture(null));

        CompletableFuture<Void> result = loginService.expire();

        assertTrue(result.isDone());
        verify(mockLoggingService).info("Attempting to expire credentials...");
        verify(mockedAuthCredentialsService).updateTokenCredentials(updateCredentialsPayload);
        verify(mockAuthStateManager).toExpired();
        verify(mockLoggingService).info("Successfully expired credentials");
        verifyNoMoreInteractions(mockedAuthCredentialsService, mockAuthStateManager);
    }

//  @Test
//  void reAuthenticateBuilderIdNoLoginOnInvalidTokenSuccess() {
//      AuthState authState = createExpiredBuilderAuthState();
//      GetSsoTokenResult expectedSsoToken = createSsoTokenResult();
//      boolean loginOnInvalidToken = false;
//
//      when(mockAuthStateManager.getAuthState()).thenReturn(authState);
//      when(mockedAuthTokenService.getSsoToken(authState.loginType(), authState.loginParams(), false))
//          .thenReturn(CompletableFuture.completedFuture(expectedSsoToken));
//      when(mockedAuthCredentialsService.updateTokenCredentials(new UpdateCredentialsPayload(expectedSsoToken.accessToken(), true)))
//          .thenReturn(CompletableFuture.completedFuture(new ResponseMessage()));
//
//      CompletableFuture<Void> result = loginService.reAuthenticate(loginOnInvalidToken);
//
//      assertTrue(result.isDone());
//      verify(mockLoggingService).info("Attempting to re-authenticate...");
//      verify(mockLoggingService).info("Successfully logged in");
//      verify(mockedAuthTokenService).getSsoToken(LoginType.BUILDER_ID, authState.loginParams(), false);
//      verify(mockedAuthCredentialsService).updateTokenCredentials(new UpdateCredentialsPayload(expectedSsoToken.accessToken(), true));
//      verify(mockAuthStateManager).toLoggedIn(LoginType.BUILDER_ID, authState.loginParams(), expectedSsoToken.id());
//  }
//
//  @Test
//  void reAuthenticateBuilderIdWithLoginOnInvalidTokenSuccess() {
//      AuthState authState = createExpiredBuilderAuthState();
//      GetSsoTokenResult expectedSsoToken = createSsoTokenResult();
//      boolean loginOnInvalidToken = true;
//
//      when(mockAuthStateManager.getAuthState()).thenReturn(authState);
//      when(mockedAuthTokenService.getSsoToken(authState.loginType(), authState.loginParams(), true))
//          .thenReturn(CompletableFuture.completedFuture(expectedSsoToken));
//      when(mockedAuthCredentialsService.updateTokenCredentials(new UpdateCredentialsPayload(expectedSsoToken.accessToken(), true)))
//          .thenReturn(CompletableFuture.completedFuture(new ResponseMessage()));
//
//      CompletableFuture<Void> result = loginService.reAuthenticate(loginOnInvalidToken);
//
//      assertTrue(result.isDone());
//      verify(mockLoggingService).info("Attempting to re-authenticate...");
//      verify(mockLoggingService).info("Successfully logged in");
//      verify(mockedAuthTokenService).getSsoToken(LoginType.BUILDER_ID, authState.loginParams(), true);
//      verify(mockedAuthCredentialsService).updateTokenCredentials(new UpdateCredentialsPayload(expectedSsoToken.accessToken(), true));
//      verify(mockAuthStateManager).toLoggedIn(LoginType.BUILDER_ID, authState.loginParams(), expectedSsoToken.id());
//  }

//  @Test
//  void reAuthenticateIdcNoLoginOnInvalidTokenSuccess() {
//      AuthState authState = createExpiredIdcAuthState();
//      SsoToken expectedSsoToken = createSsoToken();
//      boolean loginOnInvalidToken = true;
//
//      when(mockAuthStateManager.getAuthState()).thenReturn(authState);
//      when(mockSsoTokenResult.ssoToken()).thenReturn(expectedSsoToken);
//      when(mockEncryptionManager.decrypt(expectedSsoToken.accessToken())).thenReturn("-decryptedAccessToken-");
//      when(mockedAuthTokenService.getSsoToken(authState.loginType(), authState.loginParams(), true))
//          .thenReturn(CompletableFuture.completedFuture(expectedSsoToken));
//      when(mockedAuthCredentialsService.updateTokenCredentials(new UpdateCredentialsPayload(expectedSsoToken.accessToken(), true)))
//          .thenReturn(CompletableFuture.completedFuture(new ResponseMessage()));
//
//      CompletableFuture<Void> result = loginService.reAuthenticate(loginOnInvalidToken);
//
//      assertTrue(result.isDone());
//      verify(mockLoggingService).info("Attempting to re-authenticate...");
//      verify(mockLoggingService).info("Successfully logged in");
//      verify(mockedAuthTokenService).getSsoToken(LoginType.IAM_IDENTITY_CENTER, authState.loginParams(), true);
//      verify(mockedAuthCredentialsService).updateTokenCredentials(new UpdateCredentialsPayload(expectedSsoToken.accessToken(), true));
//      verify(mockAuthStateManager).toLoggedIn(LoginType.IAM_IDENTITY_CENTER, authState.loginParams(), expectedSsoToken.id());
//  }

    @Test
    void reAuthenticateIdcWithLoginOnInvalidTokenSuccess() {
        LoginIdcParams idcParams = createLoginIdcParams("test-region", "test-url");
        LoginParams loginParams = createLoginParams(idcParams);
        AuthState authState = createAuthState(AuthStateType.EXPIRED, LoginType.IAM_IDENTITY_CENTER, loginParams,
                Constants.AWS_BUILDER_ID_URL, "test-sso-token-id");
        boolean loginOnInvalidToken = false;

        when(mockAuthStateManager.getAuthState()).thenReturn(authState);
        when(mockedAuthTokenService.getSsoToken(authState.loginType(), authState.loginParams(), false))
                .thenReturn(CompletableFuture.completedFuture(expectedSsoToken));
        when(mockedAuthCredentialsService.updateTokenCredentials(expectedSsoToken.updateCredentialsParams()))
                .thenReturn(CompletableFuture.completedFuture(null));

        CompletableFuture<Void> result = loginService.reAuthenticate(loginOnInvalidToken);

        assertTrue(result.isDone());
        verify(mockLoggingService).info("Attempting to re-authenticate...");
        verify(mockLoggingService).info("Successfully logged in");
        verify(mockedAuthTokenService).getSsoToken(LoginType.IAM_IDENTITY_CENTER, authState.loginParams(), false);
        verify(mockedAuthCredentialsService).updateTokenCredentials(expectedSsoToken.updateCredentialsParams());
        verify(mockAuthStateManager).toLoggedIn(LoginType.IAM_IDENTITY_CENTER, authState.loginParams(),
                expectedSsoToken.ssoToken().id());
    }

    @Test
    void reAuthenticateWhenLoggedOutValidation() {
        AuthState authState = createAuthState(AuthStateType.LOGGED_OUT, LoginType.NONE, null, null, null);
        when(mockAuthStateManager.getAuthState()).thenReturn(authState);

        CompletableFuture<Void> result = loginService.reAuthenticate(true);

        assertTrue(result.isDone());
        verify(mockLoggingService).warn("Attempted to re-authenticate while user is in a logged out state");
        verifyNoMoreInteractions(mockedAuthTokenService, mockedAuthCredentialsService);
    }

    @Test
    void processLoginBuilderIdNoLoginOnInvalidTokenSuccess() throws Exception {
        LoginType loginType = LoginType.BUILDER_ID;
        LoginIdcParams idcParams = createLoginIdcParams("test-region", "test-url");
        LoginParams loginParams = createLoginParams(idcParams);
        AuthState authState = createAuthState(AuthStateType.LOGGED_IN, LoginType.BUILDER_ID, loginParams,
                Constants.AWS_BUILDER_ID_URL, "test-sso-token-id");

        boolean loginOnInvalidToken = false;

        when(mockedAuthTokenService.getSsoToken(loginType, loginParams, loginOnInvalidToken))
                .thenReturn(CompletableFuture.completedFuture(expectedSsoToken));
        when(mockedAuthCredentialsService.updateTokenCredentials(expectedSsoToken.updateCredentialsParams()))
                .thenReturn(CompletableFuture.completedFuture(null));

        invokeProcessLogin(loginType, loginParams, loginOnInvalidToken);

        mockedAuthUtil.verify(() -> AuthUtil.validateLoginParameters(loginType, loginParams));
        verify(mockedAuthTokenService).getSsoToken(loginType, loginParams, loginOnInvalidToken);
        verify(mockedAuthCredentialsService).updateTokenCredentials(expectedSsoToken.updateCredentialsParams());
        verify(mockAuthStateManager).toLoggedIn(loginType, loginParams, expectedSsoToken.ssoToken().id());
        verify(mockLoggingService).info("Successfully logged in");

    }

    @Test
    void processLoginIdcNoLoginOnInvalidTokenSuccess() throws Exception {
        LoginType loginType = LoginType.IAM_IDENTITY_CENTER;
        LoginIdcParams idcParams = createLoginIdcParams("test-region", "test-url");
        LoginParams loginParams = createLoginParams(idcParams);
        boolean loginOnInvalidToken = false;

        when(mockedAuthTokenService.getSsoToken(loginType, loginParams, loginOnInvalidToken))
                .thenReturn(CompletableFuture.completedFuture(expectedSsoToken));
        when(mockedAuthCredentialsService.updateTokenCredentials(expectedSsoToken.updateCredentialsParams()))
                .thenReturn(CompletableFuture.completedFuture(null));

        invokeProcessLogin(loginType, loginParams, loginOnInvalidToken);

        mockedAuthUtil.verify(() -> AuthUtil.validateLoginParameters(loginType, loginParams));
        verify(mockedAuthTokenService).getSsoToken(loginType, loginParams, loginOnInvalidToken);
        verify(mockedAuthCredentialsService).updateTokenCredentials(expectedSsoToken.updateCredentialsParams());
        verify(mockAuthStateManager).toLoggedIn(loginType, loginParams, expectedSsoToken.ssoToken().id());
        verify(mockLoggingService).info("Successfully logged in");
    }

    @Test
    void processLoginIdcWithLoginOnInvalidTokenSuccess() throws Exception {
        LoginType loginType = LoginType.IAM_IDENTITY_CENTER;
        LoginIdcParams idcParams = createLoginIdcParams("test-region", "test-url");
        LoginParams loginParams = createLoginParams(idcParams);
        boolean loginOnInvalidToken = true;

        when(mockedAuthTokenService.getSsoToken(loginType, loginParams, loginOnInvalidToken))
                .thenReturn(CompletableFuture.completedFuture(expectedSsoToken));
        when(mockedAuthCredentialsService.updateTokenCredentials(expectedSsoToken.updateCredentialsParams()))
                .thenReturn(CompletableFuture.completedFuture(null));

        invokeProcessLogin(loginType, loginParams, loginOnInvalidToken);

        mockedAuthUtil.verify(() -> AuthUtil.validateLoginParameters(loginType, loginParams));
        verify(mockedAuthTokenService).getSsoToken(loginType, loginParams, loginOnInvalidToken);
        verify(mockedAuthCredentialsService).updateTokenCredentials(expectedSsoToken.updateCredentialsParams());
        verify(mockAuthStateManager).toLoggedIn(loginType, loginParams, expectedSsoToken.ssoToken().id());
        verify(mockLoggingService).info("Successfully logged in");
    }

    private LoginParams createLoginParams(final LoginIdcParams idcParams) {
        LoginParams loginParams = mock(LoginParams.class);
        when(loginParams.getLoginIdcParams()).thenReturn(idcParams);
        return loginParams;
    }

    private LoginIdcParams createLoginIdcParams(final String region, final String url) {
        LoginIdcParams idcParams = mock(LoginIdcParams.class);
        when(idcParams.getRegion()).thenReturn(region);
        when(idcParams.getUrl()).thenReturn(url);
        return idcParams;
    }

    private void resetLoginService() {
        loginService = new DefaultLoginService.Builder()
                .withLspProvider(mockLspProvider)
                .withPluginStore(mockPluginStore)
                .withAuthStateManager(mockAuthStateManager)
                .withAuthCredentialsService(mockedAuthCredentialsService)
                .withAuthTokenService(mockedAuthTokenService)
                .build();
        loginService = spy(loginService);
    }

    private AuthState createAuthState(final AuthStateType authStateType, final LoginType loginType,
            final LoginParams loginParams, final String issuerUrl, final String ssoTokenId) {
        AuthState authState = mock(AuthState.class);
        when(authState.authStateType()).thenReturn(authStateType);
        when(authState.loginType()).thenReturn(loginType);
        when(authState.loginParams()).thenReturn(loginParams);
        when(authState.issuerUrl()).thenReturn(issuerUrl);
        when(authState.ssoTokenId()).thenReturn(ssoTokenId);

        when(authState.isLoggedIn()).thenReturn(authStateType.equals(AuthStateType.LOGGED_IN));
        when(authState.isLoggedOut()).thenReturn(authStateType.equals(AuthStateType.LOGGED_OUT));
        when(authState.isExpired()).thenReturn(authStateType.equals(AuthStateType.EXPIRED));

        return authState;
    }

    private void invokeProcessLogin(final LoginType loginType, final LoginParams loginParams,
            final boolean loginOnInvalidToken) throws Exception {
        Object processLoginFuture = loginService.processLogin(loginType, loginParams, loginOnInvalidToken);
        assertTrue(processLoginFuture instanceof CompletableFuture<?>, "Return value should be CompletableFuture");

        CompletableFuture<?> future = (CompletableFuture<?>) processLoginFuture;
        Object result = future.get();
        assertNull(result);
    }

}
