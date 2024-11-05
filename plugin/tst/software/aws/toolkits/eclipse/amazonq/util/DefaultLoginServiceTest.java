// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseMessage;
import software.amazon.awssdk.services.toolkittelemetry.model.AWSProduct;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;

import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.GetSsoTokenResult;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginDetails;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginParams;
import software.aws.toolkits.eclipse.amazonq.configuration.PluginStore;
import software.aws.toolkits.eclipse.amazonq.lsp.AmazonQLspServer;

import org.mockito.MockedStatic;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginIdcParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.UpdateProfileParams;
import software.aws.toolkits.eclipse.amazonq.lsp.encryption.LspEncryptionManager;
import software.aws.toolkits.eclipse.amazonq.lsp.model.UpdateCredentialsPayloadData;
import software.aws.toolkits.eclipse.amazonq.lsp.model.UpdateCredentialsPayload;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.providers.LspProvider;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.SsoToken;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.Profile;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.SsoSession;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.UpdateProfileOptions;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.aws.toolkits.eclipse.amazonq.util.QConstants.Q_SCOPES;

public class DefaultLoginServiceTest {


    private static DefaultLoginService loginService;
    private static LspProvider mockLspProvider;
    private static LspEncryptionManager mockEncryptionManager;
    private static AmazonQLspServer mockAmazonQServer;
    private static TestPluginStore mockPluginStore;

    @BeforeEach
    public final void setUp() {
        mockLspProvider = mock(LspProvider.class);
        mockAmazonQServer = mock(AmazonQLspServer.class);
        mockEncryptionManager = mock(LspEncryptionManager.class);
        mockPluginStore = new TestPluginStore();
    }

    @AfterEach
    public final void tearDown() {
        mockPluginStore.clear();
    }

    @Test
    public void testSuccessfulLoginIdc() {
        resetLoginService();
        LoginType loginType = LoginType.IAM_IDENTITY_CENTER;
        SsoToken mockToken = mock(SsoToken.class);
        LoginIdcParams idcParams = new LoginIdcParams();
        LoginParams loginParams = new LoginParams();
        loginParams.setLoginIdcParams(idcParams);

        doReturn(CompletableFuture.completedFuture(mockToken)).when(loginService).getToken(true);
        doReturn(CompletableFuture.completedFuture(null)).when(loginService).updateCredentials(mockToken);

        CompletableFuture<Void> result = loginService.login(loginType, loginParams);

        assertDoesNotThrow(() -> result.get());
        assertEquals("IAM_IDENTITY_CENTER", mockPluginStore.get("LOGIN_TYPE"));
        assertSame(idcParams, mockPluginStore.getObject("IDC_PARAMS", LoginIdcParams.class));
        verify(loginService).getToken(eq(true));
        verify(loginService).updateCredentials(mockToken);
    }
    @Test
    public void testSuccessfulLoginBuilderId() {
        resetLoginService();
        LoginType loginType = LoginType.BUILDER_ID;
        SsoToken mockToken = mock(SsoToken.class);
        LoginParams loginParams = new LoginParams();

        doReturn(CompletableFuture.completedFuture(mockToken)).when(loginService).getToken(true);
        doReturn(CompletableFuture.completedFuture(null)).when(loginService).updateCredentials(mockToken);

        CompletableFuture<Void> result = loginService.login(loginType, loginParams);

        assertDoesNotThrow(() -> result.get());
        assertEquals("BUILDER_ID", mockPluginStore.get("LOGIN_TYPE"));
        assertNull(mockPluginStore.getObject("IDC_PARAMS", LoginIdcParams.class));
        verify(loginService).getToken(eq(true));
        verify(loginService).updateCredentials(mockToken);
    }
    @Test
    public void testLoginThrowsException() {
        resetLoginService();
        LoginType loginType = LoginType.BUILDER_ID;
        SsoToken mockToken = mock(SsoToken.class);
        LoginParams loginParams = new LoginParams();

        try (MockedStatic<Activator> mockedActivator = mockStatic(Activator.class)) {
            LoggingService mockLogger = mockLoggingService(mockedActivator);

            doReturn(CompletableFuture.completedFuture(null)).when(loginService).getToken(true);
            CompletableFuture<Void> result = loginService.login(loginType, loginParams);

            Exception exception = assertThrows(Exception.class, result::get);
            assertTrue(exception.getCause() instanceof AmazonQPluginException);
            verify(loginService).getToken(eq(true));
            verify(loginService, never()).updateCredentials(mockToken);

            assertNull(mockPluginStore.get("LOGIN_TYPE"));
            assertNull(mockPluginStore.getObject("IDC_PARAMS", LoginIdcParams.class));
            verify(mockLogger).error(eq("Failed to sign in"), any(Throwable.class));
        }
    }

    @Test
    public void testLogoutSuccess() throws Exception {
        SsoToken mockToken = new SsoToken("id", "accesstoken");
        mockPluginStore.put("LOGIN_TYPE", "BUILDER_ID");
        mockPluginStore.put("IDC_PARAMS", "someValue");
        try (MockedStatic<AuthStatusProvider> mockedAuthStatusProvider = mockStatic(AuthStatusProvider.class)) {
            mockedAuthStatusProvider.when(() -> AuthStatusProvider.notifyAuthStatusChanged(any()))
                    .thenAnswer(invocation -> {
                        return null;
                    });
            resetLoginService();
            doReturn(CompletableFuture.completedFuture(mockToken)).when(loginService).getToken(false);
            doReturn(CompletableFuture.completedFuture(null)).when(loginService).updateCredentials(mockToken);

            assertNotNull(mockPluginStore.get("LOGIN_TYPE"));
            when(mockLspProvider.getAmazonQServer()).thenReturn(CompletableFuture.completedFuture(mockAmazonQServer));
            when(mockAmazonQServer.invalidateSsoToken(any())).thenReturn(CompletableFuture.completedFuture(null));

            CompletableFuture<Void> result = loginService.logout();
            assertDoesNotThrow(() -> result.get());
            verify(loginService).getToken(eq(false));
            verify(mockAmazonQServer).invalidateSsoToken(argThat(param ->
                    param != null
                            && (param).ssoTokenId().equals("id")
            ));
            mockedAuthStatusProvider.verify(() ->
                    AuthStatusProvider.notifyAuthStatusChanged(argThat(details ->
                            !details.getIsLoggedIn() && details.getLoginType() == LoginType.NONE
                    ))
            );
            assertNull(mockPluginStore.get("LOGIN_TYPE"));
            assertNull(mockPluginStore.get("IDC_PARAMS"));
        }
    }
    //resetting LoginService without calling login() or manually adding a loginType to the mocked PluginStore
    //will create the loginService with a LoginType of NONE
    @Test
    public void testLogoutWhileLoginTypeNone() {
        resetLoginService();
        try (MockedStatic<Activator> mockedActivator = mockStatic(Activator.class)) {
            LoggingService mockLogger = mockLoggingService(mockedActivator);

            CompletableFuture<Void> result = loginService.logout();
            assertNotNull(result);
            verify(mockLogger).warn("Attempting to invalidate token in a logged out state");
            verify(loginService, never()).getToken(false);
        }
    }
    @Test
    public void testLogoutWithNullToken() throws Exception {
        mockPluginStore.put("LOGIN_TYPE", "BUILDER_ID");
        resetLoginService();
        try (MockedStatic<Activator> mockedActivator = mockStatic(Activator.class)) {
            LoggingService mockLogger = mockLoggingService(mockedActivator);
            doReturn(CompletableFuture.completedFuture(null)).when(loginService).getToken(false);
            CompletableFuture<Void> result = loginService.logout();

            assertNotNull(result);
            assertDoesNotThrow(() -> result.get());
            verify(loginService).getToken(false);
            verify(mockLogger).warn("Attempting to invalidate token with no active auth session");
            verifyNoInteractions(mockAmazonQServer);
        }
    }
    @Test
    public void testLogoutWithException() {
        SsoToken mockToken = new SsoToken("id", "accesstoken");
        mockPluginStore.put("LOGIN_TYPE", "BUILDER_ID");
        mockPluginStore.put("IDC_PARAMS", "someValue");
        resetLoginService();
        try (MockedStatic<AuthStatusProvider> mockedAuthStatusProvider = mockStatic(AuthStatusProvider.class);
             MockedStatic<Activator> mockedActivator = mockStatic(Activator.class)) {
            mockedAuthStatusProvider.when(() -> AuthStatusProvider.notifyAuthStatusChanged(any()))
                    .thenAnswer(invocation -> {
                        return null;
                    });
            doReturn(CompletableFuture.completedFuture(mockToken)).when(loginService).getToken(false);
            doReturn(CompletableFuture.completedFuture(null)).when(loginService).updateCredentials(mockToken);
            when(mockLspProvider.getAmazonQServer()).thenReturn(CompletableFuture.completedFuture(mockAmazonQServer));
            when(mockAmazonQServer.invalidateSsoToken(any())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("test exception")));

            LoggingService mockLogger = mockLoggingService(mockedActivator);
            CompletableFuture<Void> result = loginService.logout();

            ExecutionException exception = assertThrows(ExecutionException.class, () -> result.get());
            assertTrue(exception.getCause() instanceof AmazonQPluginException);
            verify(mockLogger).error(eq("Unexpected error while invalidating token"), any(Throwable.class));
            verify(mockAmazonQServer).invalidateSsoToken(any());
            verifyNoMoreInteractions(mockAmazonQServer);
            mockedAuthStatusProvider.verifyNoInteractions();
        }
    }

    @Test
    public void testGetLoginDetailsWhileLoggedOut() throws InterruptedException {
        resetLoginService();
        try (MockedStatic<AuthStatusProvider> mockedAuthStatusProvider = mockStatic(AuthStatusProvider.class)) {
            mockedAuthStatusProvider.when(() -> AuthStatusProvider.notifyAuthStatusChanged(any()))
                    .thenAnswer(invocation -> {
                        return null;
                    });
            CompletableFuture<LoginDetails> result = loginService.getLoginDetails();
            LoginDetails loginDetails = assertDoesNotThrow(() -> result.get());
            assertFalse(loginDetails.getIsLoggedIn());
            assertEquals(LoginType.NONE, loginDetails.getLoginType());
            assertNull(loginDetails.getIssuerUrl());

            mockedAuthStatusProvider.verify(() -> AuthStatusProvider.notifyAuthStatusChanged(any()));
        }
    }
    @Test
    public void testGetLoginDetailsSuccessWithBuilderId() {
        mockPluginStore.put("LOGIN_TYPE", "BUILDER_ID");
        resetLoginService();
        SsoToken mockToken = mock(SsoToken.class);
        try (MockedStatic<AuthStatusProvider> mockedAuthStatusProvider = mockStatic(AuthStatusProvider.class)) {
            mockedAuthStatusProvider.when(() -> AuthStatusProvider.notifyAuthStatusChanged(any()))
                    .thenAnswer(invocation -> {
                        return null;
                    });
            doReturn(CompletableFuture.completedFuture(mockToken)).when(loginService).getToken(false);
            CompletableFuture<LoginDetails> result = loginService.getLoginDetails();

            LoginDetails loginDetails = assertDoesNotThrow(() -> result.get());
            assertTrue(loginDetails.getIsLoggedIn());
            assertEquals(LoginType.BUILDER_ID, loginDetails.getLoginType());
            assertEquals(loginDetails.getIssuerUrl(), Constants.AWS_BUILDER_ID_URL);
            verify(loginService).getToken(eq(false));
            mockedAuthStatusProvider.verify(() -> AuthStatusProvider.notifyAuthStatusChanged(any()));
        }
    }
    @Test
    public void testGetLoginDetailsSuccessWithIdc() {
        mockPluginStore.put("LOGIN_TYPE", "IAM_IDENTITY_CENTER");
        LoginIdcParams idcParams = new LoginIdcParams();
        idcParams.setUrl("idcTestUrl");
        mockPluginStore.putObject(Constants.LOGIN_IDC_PARAMS_KEY, idcParams);
        resetLoginService();
        SsoToken mockToken = mock(SsoToken.class);
        try (MockedStatic<AuthStatusProvider> mockedAuthStatusProvider = mockStatic(AuthStatusProvider.class)) {
            mockedAuthStatusProvider.when(() -> AuthStatusProvider.notifyAuthStatusChanged(any()))
                    .thenAnswer(invocation -> {
                        return null;
                    });
            doReturn(CompletableFuture.completedFuture(mockToken)).when(loginService).getToken(false);
            CompletableFuture<LoginDetails> result = loginService.getLoginDetails();

            LoginDetails loginDetails = assertDoesNotThrow(() -> result.get());
            assertTrue(loginDetails.getIsLoggedIn());
            assertEquals(LoginType.IAM_IDENTITY_CENTER, loginDetails.getLoginType());
            assertEquals("idcTestUrl", loginDetails.getIssuerUrl());
            verify(loginService).getToken(eq(false));
            mockedAuthStatusProvider.verify(() -> AuthStatusProvider.notifyAuthStatusChanged(any()));
        }
    }
    @Test
    public void testGetLoginDetailsThrowsException() {
        mockPluginStore.put("LOGIN_TYPE", "BUILDER_ID");
        resetLoginService();
        try (MockedStatic<AuthStatusProvider> mockedAuthStatusProvider = mockStatic(AuthStatusProvider.class);
             MockedStatic<Activator> mockedActivator = mockStatic(Activator.class)) {
            mockedAuthStatusProvider.when(() -> AuthStatusProvider.notifyAuthStatusChanged(any()))
                    .thenAnswer(invocation -> {
                        return null;
                    });
            LoggingService mockLogger = mockLoggingService(mockedActivator);
            RuntimeException testException = new RuntimeException("Test exception");
            doReturn(CompletableFuture.failedFuture(testException)).when(loginService).getToken(false);
            CompletableFuture<LoginDetails> result = loginService.getLoginDetails();
            LoginDetails loginDetails = assertDoesNotThrow(() -> result.get());

            verify(mockLogger).error(eq("Failed to check login status"), any(Throwable.class));
            assertFalse(loginDetails.getIsLoggedIn());
            assertEquals(LoginType.NONE, loginDetails.getLoginType());
            assertNull(loginDetails.getIssuerUrl());
            verify(loginService).getToken(eq(false));
            mockedAuthStatusProvider.verify(() -> AuthStatusProvider.notifyAuthStatusChanged(any()));
        }
    }
    @Test
    public void testGetLoginDetailsSsoTokenNull() {
        mockPluginStore.put("LOGIN_TYPE", "BUILDER_ID");
        resetLoginService();
        try (MockedStatic<AuthStatusProvider> mockedAuthStatusProvider = mockStatic(AuthStatusProvider.class)) {
            mockedAuthStatusProvider.when(() -> AuthStatusProvider.notifyAuthStatusChanged(any()))
                    .thenAnswer(invocation -> {
                        return null;
                    });
            doReturn(CompletableFuture.completedFuture(null)).when(loginService).getToken(false);
            CompletableFuture<LoginDetails> result = loginService.getLoginDetails();
            LoginDetails loginDetails = assertDoesNotThrow(() -> result.get());

            assertFalse(loginDetails.getIsLoggedIn());
            assertEquals(LoginType.NONE, loginDetails.getLoginType());
            assertNull(loginDetails.getIssuerUrl());
            verify(loginService).getToken(eq(false));
            mockedAuthStatusProvider.verify(() -> AuthStatusProvider.notifyAuthStatusChanged(any()));
        }
    }
    @Test
    public void testReAuthenticateWithException() {
        mockPluginStore.put("LOGIN_TYPE", "BUILDER_ID");
        resetLoginService();
        try (MockedStatic<Activator> mockedActivator = mockStatic(Activator.class)) {
            LoggingService mockLogger = mockLoggingService(mockedActivator);
            doReturn(CompletableFuture.failedFuture(new RuntimeException(""))).when(loginService).login(any(), any());
            boolean result = assertDoesNotThrow(() -> loginService.reAuthenticate().get());
            verify(mockLogger).error(eq("Failed to reauthenticate"), any(Throwable.class));
            assertFalse(result);
        }
    }
    @Test
    public void testSuccessfulReAuthenticate() {
        assertTrue(reAuthenticateWith(LoginType.BUILDER_ID.name()));
        assertTrue(reAuthenticateWith(LoginType.NONE.name()));
        assertTrue(reAuthenticateWith(LoginType.IAM_IDENTITY_CENTER.name()));
    }
    @Test
    public void testGetTokenSuccessWithIdc() {
        //set up login type
        mockPluginStore.put("LOGIN_TYPE", "IAM_IDENTITY_CENTER");
        LoginIdcParams idcParams = mock(LoginIdcParams.class);
        when(idcParams.getUrl()).thenReturn("testUrl");
        when(idcParams.getRegion()).thenReturn("testRegion");
        mockPluginStore.putObject(Constants.LOGIN_IDC_PARAMS_KEY, idcParams);

        //set up expected results of fetching ssoToken
        SsoToken expectedToken = new SsoToken("id", "accessToken");
        GetSsoTokenResult mockSsoTokenResult = mock(GetSsoTokenResult.class);
        when(mockSsoTokenResult.ssoToken()).thenReturn(expectedToken);

        //set up server calls
        resetLoginService();
        when(mockLspProvider.getAmazonQServer()).thenReturn(CompletableFuture.completedFuture(mockAmazonQServer));
        when(mockAmazonQServer.getSsoToken(any())).thenReturn(CompletableFuture.completedFuture(mockSsoTokenResult));
        when(mockAmazonQServer.updateProfile(any())).thenReturn(CompletableFuture.completedFuture(null));

        try (MockedStatic<AuthStatusProvider> mockedAuthStatusProvider = mockStatic(AuthStatusProvider.class);
             MockedStatic<Activator> mockedActivator = mockStatic(Activator.class)) {
            LoggingService mockLogger = mockLoggingService(mockedActivator);

            //Test getToken with triggerSignIn false
            CompletableFuture<SsoToken> result = loginService.getToken(false);
            SsoToken actualToken = assertDoesNotThrow(() -> result.get());
            assertEquals(expectedToken, actualToken);

            verify(mockLspProvider).getAmazonQServer();
            verify(mockAmazonQServer).getSsoToken(argThat(params ->
                    params.source().kind().equals("IamIdentityCenter")
                            && params.source().ssoRegistrationScopes() == null
                            && params.source().profileName().equals(Constants.IDC_PROFILE_NAME)
                            && params.clientName().equals(AWSProduct.AMAZON_Q_FOR_ECLIPSE.toString())
                            && !params.options().loginOnInvalidToken()
            ));
            verify(mockAmazonQServer, never()).updateProfile(any());
            mockedAuthStatusProvider.verifyNoInteractions();

            //Test getToken with triggerSignIn true
            resetServerAndProvider();
            resetLoginService();
            when(mockLspProvider.getAmazonQServer()).thenReturn(CompletableFuture.completedFuture(mockAmazonQServer));
            when(mockAmazonQServer.getSsoToken(any())).thenReturn(CompletableFuture.completedFuture(mockSsoTokenResult));
            when(mockAmazonQServer.updateProfile(any())).thenReturn(CompletableFuture.completedFuture(null));

            CompletableFuture<SsoToken> resultSignInTrue = loginService.getToken(true);
            SsoToken actualTokenSignInTrue = assertDoesNotThrow(() -> resultSignInTrue.get());
            assertEquals(expectedToken, actualTokenSignInTrue);
            verify(mockLspProvider).getAmazonQServer();
            verify(mockAmazonQServer).getSsoToken(any());
            verify(mockAmazonQServer).updateProfile(argThat(this::verifyProfileSetupSuccess));

            mockedAuthStatusProvider.verify(() -> AuthStatusProvider.notifyAuthStatusChanged(argThat(loginDetails ->
                    loginDetails.getIsLoggedIn()
                            && loginDetails.getLoginType() == LoginType.IAM_IDENTITY_CENTER
                            && loginDetails.getIssuerUrl().equals("testUrl")
            )));

            //Test getToken with triggerSignIn true but updateProfile failure
            resetServerAndProvider();
            resetLoginService();
            when(mockLspProvider.getAmazonQServer()).thenReturn(CompletableFuture.completedFuture(mockAmazonQServer));
            when(mockAmazonQServer.getSsoToken(any())).thenReturn(CompletableFuture.completedFuture(mockSsoTokenResult));
            when(mockAmazonQServer.updateProfile(any())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("")));

            CompletableFuture<SsoToken> resultSignInProfileFailure = loginService.getToken(true);
            SsoToken actualTokenProfileFailure = assertDoesNotThrow(() -> resultSignInProfileFailure.get());
            assertEquals(expectedToken, actualTokenProfileFailure);
            verify(mockAmazonQServer).updateProfile(any());
            verify(mockLogger).error(eq("Failed to update profile"), any(Throwable.class));

            //Second notification of AuthStatusChanged
            mockedAuthStatusProvider.verify(() -> AuthStatusProvider.notifyAuthStatusChanged(argThat(loginDetails ->
                    loginDetails.getIsLoggedIn()
                            && loginDetails.getLoginType() == LoginType.IAM_IDENTITY_CENTER
                            && loginDetails.getIssuerUrl().equals("testUrl")
            )), times(2));
        }
    }

    @Test
    public void testGetTokenSuccessWithBuilderId() {
        mockPluginStore.put("LOGIN_TYPE", "BUILDER_ID");
        SsoToken expectedToken = new SsoToken("id", "accessToken");
        GetSsoTokenResult mockSsoTokenResult = mock(GetSsoTokenResult.class);
        when(mockSsoTokenResult.ssoToken()).thenReturn(expectedToken);

        resetLoginService();
        when(mockLspProvider.getAmazonQServer()).thenReturn(CompletableFuture.completedFuture(mockAmazonQServer));
        when(mockAmazonQServer.getSsoToken(any())).thenReturn(CompletableFuture.completedFuture(mockSsoTokenResult));
        try (MockedStatic<AuthStatusProvider> mockedAuthStatusProvider = mockStatic(AuthStatusProvider.class)) {
            CompletableFuture<SsoToken> result = loginService.getToken(false);
            SsoToken actualToken = assertDoesNotThrow(() -> result.get());
            assertEquals(expectedToken, actualToken);

            verify(mockLspProvider).getAmazonQServer();
            verify(mockAmazonQServer).getSsoToken(argThat(params ->
                    params.source().kind().equals(LoginType.BUILDER_ID.getValue())
                            && params.source().ssoRegistrationScopes() == Q_SCOPES
                            && params.source().profileName() == null
                            && params.clientName().equals(AWSProduct.AMAZON_Q_FOR_ECLIPSE.toString())
                            && !params.options().loginOnInvalidToken()
            ));
            mockedAuthStatusProvider.verifyNoInteractions();
        }
    }
    @Test
    public void testGetTokenWithException() {
        mockPluginStore.put("LOGIN_TYPE", "BUILDER_ID");
        resetLoginService();
        RuntimeException expectedException = new RuntimeException("Test exception");
        when(mockLspProvider.getAmazonQServer()).thenReturn(CompletableFuture.failedFuture(expectedException));

        try (MockedStatic<Activator> mockedActivator = mockStatic(Activator.class);
             MockedStatic<AuthStatusProvider> mockedAuthStatusProvider = mockStatic(AuthStatusProvider.class)) {
            mockedAuthStatusProvider.when(() -> AuthStatusProvider.notifyAuthStatusChanged(any()))
                    .thenAnswer(invocation -> {
                        return null;
                    });
            LoggingService mockLogger = mockLoggingService(mockedActivator);
            CompletableFuture<SsoToken> result = loginService.getToken(false);
            Exception exception = assertThrows(Exception.class, () -> result.get());
            assertTrue(exception.getCause() instanceof AmazonQPluginException);

            verify(mockLogger).error(eq("Failed to fetch SSO token from LSP"), any(Throwable.class));
            mockedAuthStatusProvider.verify(() -> AuthStatusProvider.notifyAuthStatusChanged(argThat(loginDetails ->
                    !loginDetails.getIsLoggedIn()
                            && loginDetails.getLoginType() == LoginType.NONE
            )));
        }
    }
    @Test
    public void testUpdateCredentialsSuccess() {
        resetLoginService();
        SsoToken mockSsoToken = mock(SsoToken.class);
        String expectedToken = "decryptedAccessToken";
        String expectedEncryptedData = "someStringOfEncryptedData";
        updateCredentialsTestHelper(mockSsoToken, false);

        CompletableFuture<ResponseMessage> result = loginService.updateCredentials(mockSsoToken);
        assertNotNull(result);
        assertDoesNotThrow(() -> result.get());

        verify(mockSsoToken).accessToken();
        verify(mockLspProvider).getAmazonQServer();
        verify(mockAmazonQServer).updateTokenCredentials(argThat(payload -> {
            return expectedEncryptedData.equals(payload.data()) && payload.encrypted();
        }));
    }
    @Test
    public void testUpdateCredentialsWithException() {
        resetLoginService();
        SsoToken mockSsoToken = mock(SsoToken.class);
        updateCredentialsTestHelper(mockSsoToken, true);

        try (MockedStatic<Activator> mockedActivator = mockStatic(Activator.class)) {
            LoggingService mockLogger = mockLoggingService(mockedActivator);
            CompletableFuture<ResponseMessage> result = loginService.updateCredentials(mockSsoToken);
            assertNotNull(result);
            Exception exception = assertThrows(Exception.class, () -> result.get());

            verify(mockSsoToken).accessToken();
            verify(mockLspProvider).getAmazonQServer();
            assertTrue(exception.getCause() instanceof AmazonQPluginException);

            verify(mockLogger).error(eq("Failed to update credentials with AmazonQ server"), any(RuntimeException.class));
        }
    }
    private void updateCredentialsTestHelper(final SsoToken mockSsoToken, final boolean throwsException) {
        when(mockSsoToken.accessToken()).thenReturn("encryptedAccessToken");
        when(mockEncryptionManager.decrypt("encryptedAccessToken")).thenReturn("-decryptedAccessToken-");
        when(mockEncryptionManager.encrypt(any(UpdateCredentialsPayloadData.class))).thenReturn("someStringOfEncryptedData");
        when(mockLspProvider.getAmazonQServer()).thenReturn(CompletableFuture.completedFuture(mockAmazonQServer));
        if (throwsException) {
            when(mockAmazonQServer.updateTokenCredentials(any(UpdateCredentialsPayload.class)))
                    .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Test exception")));
        } else {
            when(mockAmazonQServer.updateTokenCredentials(any(UpdateCredentialsPayload.class)))
                    .thenReturn(CompletableFuture.completedFuture(new ResponseMessage()));
        }
    }

    //    some tests require loginService being reset in try blocks or after unique setups
//    thus method to reset loginSerivce instead of using BeforeEach
    private void resetLoginService() {
        loginService = new DefaultLoginService.Builder()
                .withLspProvider(mockLspProvider)
                .withPluginStore(mockPluginStore)
                .withEncryptionManager(mockEncryptionManager)
                .build();
        loginService = spy(loginService);
    }

    private void resetServerAndProvider() {
        mockLspProvider = mock(LspProvider.class);
        mockAmazonQServer = mock(AmazonQLspServer.class);
    }

    private boolean verifyProfileSetupSuccess(final UpdateProfileParams params) {
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
                && ssoSession.getSsoSessionSettings().ssoRegistrationScopes() == Q_SCOPES
                && options.createNonexistentProfile()
                && options.createNonexistentSsoSession()
                && options.ensureSsoAccountAccessScope()
                && !options.updateSharedSsoSession();
    }

    private boolean reAuthenticateWith(final String loginType) {
        mockPluginStore.put("LOGIN_TYPE", loginType);
        try (MockedStatic<Activator> mockedActivator = mockStatic(Activator.class)) {
            LoggingService mockLogger = mockLoggingService(mockedActivator);
            resetLoginService();
            doReturn(CompletableFuture.completedFuture(null)).when(loginService).login(any(LoginType.class), nullable(LoginParams.class));
            boolean result = assertDoesNotThrow(() -> loginService.reAuthenticate().get());
            verify(mockLogger).info("Attempting to re-authenticate using login type " + loginType);
            verify(mockLogger).info("Successfully reauthenticated");
            return result;
        }
    }

    private LoggingService mockLoggingService(final MockedStatic<Activator> mockedActivator) {
        LoggingService mockLogger = mock(LoggingService.class);
        mockedActivator.when(() -> Activator.getLogger()).thenReturn(mockLogger);
        return mockLogger;
    }

    final class TestPluginStore implements PluginStore {
        private final Map<String, Object> store = new HashMap<>();

        public void put(final String key, final String value) {
            store.put(key, value);
        }

        public String get(final String key) {
            return (String) store.get(key);
        }
        public void remove(final String key) {
            store.remove(key);
            return;
        }

        public <T> void putObject(final String key, final T value) {
            store.put(key, value);
        }
        public void addChangeListener(final IPreferenceChangeListener prefChangeListener) {
            //
        }

        @SuppressWarnings("unchecked")
        public <T> T getObject(final String key, final Class<T> type) {
            Object value = store.get(key);
            if (type.isInstance(value)) {
                return (T) value;
            }
            return null;
        }

        public void clear() {
            store.clear();
        }
    }
}
