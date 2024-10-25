// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import software.aws.toolkits.eclipse.amazonq.providers.LspProvider;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.SsoToken;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.prefs.PreferenceChangeListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginDetails;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginIdcParams;

import software.aws.toolkits.eclipse.amazonq.configuration.PluginStore;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.lsp.AuthLspServer;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.InvalidateSsoTokenParams;

class DefaultLoginServiceTest {

    private static DefaultLoginService loginService;

    @Mock
    private static LspProvider mockLspProvider;

    @Mock
    private static AuthLspServer mockAuthServer;
    private static TestPluginStore mockPluginStore;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mockPluginStore = new TestPluginStore();
    }

    @AfterEach
    public void tearDown() {
        mockPluginStore.clear();
    }

   @Test
   public void testSucessfulLogin() {
       resetLoginService();
       LoginType loginType = LoginType.BUILDER_ID;
       SsoToken mockToken = mock(SsoToken.class);
       LoginIdcParams idcParams = new LoginIdcParams();
       LoginParams loginParams = new LoginParams();
       loginParams.setLoginIdcParams(idcParams);

       try (MockedStatic<CredentialUtils> mockedCredentialUtils = mockStatic(CredentialUtils.class)) {
           setUpCredentialUtils(mockedCredentialUtils, mockToken, false, true);
           CompletableFuture<Void> result = loginService.login(loginType, loginParams);

           assertDoesNotThrow(() -> result.get());
           assertEquals("BUILDER_ID", mockPluginStore.get("LOGIN_TYPE"));
           assertSame(idcParams, mockPluginStore.getObject("IDC_PARAMS", LoginIdcParams.class));
           mockedCredentialUtils.verify(() -> CredentialUtils.getToken(eq(mockLspProvider), any(), eq(loginParams), eq(true)));
           mockedCredentialUtils.verify(() -> CredentialUtils.updateCredentials(mockLspProvider, mockToken));
       }
   }

   @Test
   public void testLoginWithException() {
       resetLoginService();
       LoginType loginType = LoginType.BUILDER_ID;
       LoginParams loginParams = new LoginParams();

        try (MockedStatic<CredentialUtils> mockedCredentialUtils = mockStatic(CredentialUtils.class);
        MockedStatic<Activator> mockedActivator = mockStatic(Activator.class)) {
            LoggingService mockLogger = mockLoggingService(mockedActivator);
            setUpCredentialUtils(mockedCredentialUtils, null, true, true);
            CompletableFuture<Void> result = loginService.login(loginType, loginParams);

            Exception exception = assertThrows(Exception.class, () -> result.get());
            assertTrue(exception.getCause() instanceof AmazonQPluginException);
            mockedCredentialUtils.verify(() -> CredentialUtils.getToken(eq(mockLspProvider), any(), eq(loginParams), eq(true)), times(1));
            mockedCredentialUtils.verify(() -> CredentialUtils.updateCredentials(eq(mockLspProvider), any()), never());
            verify(mockLogger).error(eq("Failed to sign in"), any(Throwable.class));
        }
   }
   @Test
   //updateToken is called when initializing loginService in resetLoginService()
   public void testUpdateTokenWhileLoggedOut() {
       try (MockedStatic<CredentialUtils> mockedCredentialUtils = mockStatic(CredentialUtils.class)) {
           resetLoginService();
           assertNotNull(loginService);
           mockedCredentialUtils.verifyNoInteractions();
       }
   }

   @Test
   public void testUpdateTokenSuccess() {
       mockPluginStore.put("LOGIN_TYPE", "BUILDER_ID");
       SsoToken mockToken = mock(SsoToken.class);
       try (MockedStatic<CredentialUtils> mockedCredentialUtils = mockStatic(CredentialUtils.class)) {
            setUpCredentialUtils(mockedCredentialUtils, mockToken, false, false);
            resetLoginService();

            mockedCredentialUtils.verify(() -> CredentialUtils.getToken(eq(mockLspProvider), any(), eq(null), eq(false)), times(1));
            mockedCredentialUtils.verify(() -> CredentialUtils.updateCredentials(mockLspProvider, mockToken), times(1));
       }
   }

    @Test
    public void testUpdateTokenWithException() {
        mockPluginStore.put("LOGIN_TYPE", "BUILDER_ID");
        try (MockedStatic<CredentialUtils> mockedCredentialUtils = mockStatic(CredentialUtils.class);
        MockedStatic<Activator> mockedActivator = mockStatic(Activator.class)) {

            LoggingService mockLogger = mockLoggingService(mockedActivator);
            setUpCredentialUtils(mockedCredentialUtils, null, true, false);
            resetLoginService();

            mockedCredentialUtils.verify(() -> CredentialUtils.getToken(eq(mockLspProvider), any(), eq(null), eq(false)), times(1));
            mockedCredentialUtils.verify(() -> CredentialUtils.updateCredentials(eq(mockLspProvider), any()), never());

            verify(mockLogger).error(eq("Failed to update token"), any(Throwable.class));
        }
    }

   @Test
   public void testLogoutSuccess() throws Exception {
    SsoToken mockToken = new SsoToken("id", "accesstoken");
    mockPluginStore.put("LOGIN_TYPE", "BUILDER_ID");
    mockPluginStore.put("IDC_PARAMS", "someValue");
    try (MockedStatic<CredentialUtils> mockedCredentialUtils = mockStatic(CredentialUtils.class);
        MockedStatic<AuthStatusProvider> mockedAuthStatusProvider = mockStatic(AuthStatusProvider.class)) {

        mockedAuthStatusProvider.when(() -> AuthStatusProvider.notifyAuthStatusChanged(any()))
        .thenAnswer(invocation -> {
            return null;
        });
        setUpCredentialUtils(mockedCredentialUtils, mockToken, false, false);
        resetLoginService();

        assertNotNull(mockPluginStore.get("LOGIN_TYPE"));
        when(mockLspProvider.getAuthServer()).thenReturn(CompletableFuture.completedFuture(mockAuthServer));
        when(mockAuthServer.invalidateSsoToken(any())).thenReturn(CompletableFuture.completedFuture(null));

        CompletableFuture<Void> result = loginService.logout();
        assertDoesNotThrow(() -> result.get());
        mockedCredentialUtils.verify(() -> CredentialUtils.getToken(eq(mockLspProvider), eq(LoginType.BUILDER_ID), any(), eq(false)), times(2));

        verify(mockAuthServer).invalidateSsoToken(argThat(param ->
                param instanceof InvalidateSsoTokenParams
                && ((InvalidateSsoTokenParams) param).ssoTokenId().equals("id")
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
    try (MockedStatic<CredentialUtils> mockedCredentialUtils = mockStatic(CredentialUtils.class)) {
        mockedCredentialUtils.when(() -> CredentialUtils.getToken(eq(mockLspProvider), any(), any(), eq(false)))
        .thenReturn(CompletableFuture.completedFuture(null));
        resetLoginService();
        CompletableFuture<Void> result = loginService.logout();
        assertNotNull(result);
        mockedCredentialUtils.verifyNoInteractions();
    }
   }

    @Test
    public void testLogoutWithNullToken() throws Exception {
        SsoToken mockToken = new SsoToken("id", "accesstoken");
        mockPluginStore.put("LOGIN_TYPE", "BUILDER_ID");
        try (MockedStatic<CredentialUtils> mockedCredentialUtils = mockStatic(CredentialUtils.class);
        MockedStatic<Activator> mockedActivator = mockStatic(Activator.class)) {
            LoggingService mockLogger = mockLoggingService(mockedActivator);
            setUpCredentialUtils(mockedCredentialUtils, mockToken, false, false);
            resetLoginService();
            mockedCredentialUtils.when(() -> CredentialUtils.getToken(eq(mockLspProvider), any(), any(), eq(false)))
            .thenReturn(CompletableFuture.completedFuture(null));
            CompletableFuture<Void> result = loginService.logout();

            assertNotNull(result);
            assertDoesNotThrow(() -> result.get());
            mockedCredentialUtils.verify(() -> CredentialUtils.getToken(eq(mockLspProvider), any(), any(), eq(false)), times(2));
            verify(mockLogger).warn("Attempting to invalidate token with no active auth session");
            verifyNoInteractions(mockAuthServer);
        }
    }
    @Test
    public void testLogoutWithException() {
        SsoToken mockToken = new SsoToken("id", "accesstoken");
        mockPluginStore.put("LOGIN_TYPE", "BUILDER_ID");
        mockPluginStore.put("IDC_PARAMS", "someValue");
        try (MockedStatic<CredentialUtils> mockedCredentialUtils = mockStatic(CredentialUtils.class);
            MockedStatic<AuthStatusProvider> mockedAuthStatusProvider = mockStatic(AuthStatusProvider.class);
            MockedStatic<Activator> mockedActivator = mockStatic(Activator.class)) {

            mockedAuthStatusProvider.when(() -> AuthStatusProvider.notifyAuthStatusChanged(any()))
            .thenAnswer(invocation -> {
                return null;
            });
            setUpCredentialUtils(mockedCredentialUtils, mockToken, false, false);
            resetLoginService();
            when(mockLspProvider.getAuthServer()).thenReturn(CompletableFuture.completedFuture(mockAuthServer));
            when(mockAuthServer.invalidateSsoToken(any())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("test exception")));

            LoggingService mockLogger = mockLoggingService(mockedActivator);
            CompletableFuture<Void> result = loginService.logout();

            ExecutionException exception = assertThrows(ExecutionException.class, () -> result.get());
            assertTrue(exception.getCause() instanceof AmazonQPluginException);
            verify(mockLogger).error(eq("Unexpected error while invalidating token"), any(Throwable.class));
            verify(mockAuthServer).invalidateSsoToken(any());
            verifyNoMoreInteractions(mockAuthServer);
            mockedAuthStatusProvider.verifyNoInteractions();
        }
    }

   @Test
   public void testGetLoginDetailsWhileLoggedOut() throws InterruptedException {
       try (MockedStatic<AuthStatusProvider> mockedAuthStatusProvider = mockStatic(AuthStatusProvider.class)) {
            mockedAuthStatusProvider.when(() -> AuthStatusProvider.notifyAuthStatusChanged(any()))
            .thenAnswer(invocation -> {
                return null;
            });
            resetLoginService();
            CompletableFuture<LoginDetails> result = loginService.getLoginDetails();
            LoginDetails loginDetails = assertDoesNotThrow(() -> result.get());
            assertFalse(loginDetails.getIsLoggedIn());
            assertEquals(LoginType.NONE, loginDetails.getLoginType());

            mockedAuthStatusProvider.verify(() -> AuthStatusProvider.notifyAuthStatusChanged(any()));
       }
   }

    @Test
    public void testGetLoginDetailsSuccess() {
        mockPluginStore.put("LOGIN_TYPE", "BUILDER_ID");
        SsoToken mockToken = mock(SsoToken.class);
        try (MockedStatic<CredentialUtils> mockedCredentialUtils = mockStatic(CredentialUtils.class);
            MockedStatic<AuthStatusProvider> mockedAuthStatusProvider = mockStatic(AuthStatusProvider.class)) {
            mockedAuthStatusProvider.when(() -> AuthStatusProvider.notifyAuthStatusChanged(any()))
            .thenAnswer(invocation -> {
                return null;
            });
            setUpCredentialUtils(mockedCredentialUtils, mockToken, false, false);
            resetLoginService();

            CompletableFuture<LoginDetails> result = loginService.getLoginDetails();

            LoginDetails loginDetails = assertDoesNotThrow(() -> result.get());
            assertTrue(loginDetails.getIsLoggedIn());
            assertEquals(LoginType.BUILDER_ID, loginDetails.getLoginType());
            mockedCredentialUtils.verify(() -> CredentialUtils.getToken(eq(mockLspProvider), any(), eq(null), eq(false)), times(2));
            mockedCredentialUtils.verify(() -> CredentialUtils.updateCredentials(mockLspProvider, mockToken), times(1));
            mockedAuthStatusProvider.verify(() -> AuthStatusProvider.notifyAuthStatusChanged(any()));
        }
    }

   @Test
   public void testGetLoginDetailsWithException() {
    mockPluginStore.put("LOGIN_TYPE", "BUILDER_ID");
    SsoToken mockToken = mock(SsoToken.class);
    try (MockedStatic<CredentialUtils> mockedCredentialUtils = mockStatic(CredentialUtils.class);
        MockedStatic<AuthStatusProvider> mockedAuthStatusProvider = mockStatic(AuthStatusProvider.class);
        MockedStatic<Activator> mockedActivator = mockStatic(Activator.class)) {
            LoggingService mockLogger = mockLoggingService(mockedActivator);
            mockedAuthStatusProvider.when(() -> AuthStatusProvider.notifyAuthStatusChanged(any()))
            .thenAnswer(invocation -> {
                return null;
            });
            setUpCredentialUtils(mockedCredentialUtils, mockToken, false, false);
            resetLoginService();
            setUpCredentialUtils(mockedCredentialUtils, mockToken, true, false);

            CompletableFuture<LoginDetails> result = loginService.getLoginDetails();
            LoginDetails loginDetails = assertDoesNotThrow(() -> result.get());

            verify(mockLogger).error(eq("Failed to check login status"), any(Throwable.class));
            assertFalse(loginDetails.getIsLoggedIn());
            assertEquals(LoginType.NONE, loginDetails.getLoginType());
            mockedCredentialUtils.verify(() -> CredentialUtils.getToken(eq(mockLspProvider), any(), eq(null), eq(false)), times(2));
            mockedCredentialUtils.verify(() -> CredentialUtils.updateCredentials(mockLspProvider, mockToken), times(1));
            mockedAuthStatusProvider.verify(() -> AuthStatusProvider.notifyAuthStatusChanged(any()), never());
    }
   }

    //some tests require loginService being reset in try blocks or after unique setups
    //thus method to reset loginSerivce instead of using BeforeEach
    private void resetLoginService() {
        loginService = new DefaultLoginService.Builder()
            .withLspProvider(mockLspProvider)
            .withPluginStore(mockPluginStore)
            .build();
    }
    private LoggingService mockLoggingService(final MockedStatic<Activator> mockedActivator) {
        LoggingService mockLogger = mock(LoggingService.class);
        mockedActivator.when(() -> Activator.getLogger()).thenReturn(mockLogger);
        doNothing().when(mockLogger).error(anyString(), any(Exception.class));
        return mockLogger;
    }
    private void setUpCredentialUtils(
        final MockedStatic<CredentialUtils> mockedCredentialUtils,
        final SsoToken mockToken,
        final boolean throwsException,
        final boolean triggerSignIn) {
        if (throwsException) {
            RuntimeException testException = new RuntimeException("Test exception");
            mockedCredentialUtils.when(() -> CredentialUtils.getToken(eq(mockLspProvider), any(), any(), eq(triggerSignIn)))
            .thenReturn(CompletableFuture.failedFuture(testException));
        } else {
            mockedCredentialUtils.when(() -> CredentialUtils.getToken(eq(mockLspProvider), any(), any(), eq(triggerSignIn)))
            .thenReturn(CompletableFuture.completedFuture(mockToken));
            mockedCredentialUtils.when(() -> CredentialUtils.updateCredentials(mockLspProvider, mockToken))
            .thenReturn(CompletableFuture.completedFuture(null));
        }
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
        public void addChangeListener(final PreferenceChangeListener prefChangeListener) {
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
