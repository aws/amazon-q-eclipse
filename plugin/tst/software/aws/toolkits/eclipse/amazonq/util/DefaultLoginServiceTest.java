// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import java.util.prefs.PreferenceChangeListener;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginParams;
import software.aws.toolkits.eclipse.amazonq.configuration.PluginStore;
import software.aws.toolkits.eclipse.amazonq.lsp.AmazonQLspServer;

import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginIdcParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;
import software.aws.toolkits.eclipse.amazonq.providers.LspProvider;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.SsoToken;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import static org.mockito.Mockito.when;

public class DefaultLoginServiceTest {
    private static DefaultLoginService loginService;

    @Mock
    private static LspProvider mockLspProvider;

    private static AmazonQLspServer mockAmazonQServer;

    private static TestPluginStore mockPluginStore;

    @BeforeEach
    public final void setUp() {
//        MockitoAnnotations.openMocks(this);
        mockLspProvider = mock(LspProvider.class);
        mockAmazonQServer = mock(AmazonQLspServer.class);
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

        try (MockedStatic<CredentialUtils> mockedCredentialUtils = mockStatic(CredentialUtils.class)) {
            setUpCredentialUtils(mockedCredentialUtils, mockToken, false, true);
            CompletableFuture<Void> result = loginService.login(loginType, loginParams);

            assertDoesNotThrow(() -> result.get());
            assertEquals("IAM_IDENTITY_CENTER", mockPluginStore.get("LOGIN_TYPE"));
            assertSame(idcParams, mockPluginStore.getObject("IDC_PARAMS", LoginIdcParams.class));
            mockedCredentialUtils.verify(() -> CredentialUtils.getToken(eq(mockLspProvider), any(), eq(loginParams), eq(true)));
            mockedCredentialUtils.verify(() -> CredentialUtils.updateCredentials(mockLspProvider, mockToken));
        }
    }
    @Test
    public void testSuccessfulLoginBuilderId() {
        resetLoginService();
        LoginType loginType = LoginType.BUILDER_ID;
        SsoToken mockToken = mock(SsoToken.class);
        LoginParams loginParams = new LoginParams();

        try (MockedStatic<CredentialUtils> mockedCredentialUtils = mockStatic(CredentialUtils.class)) {
            setUpCredentialUtils(mockedCredentialUtils, mockToken, false, true);
            CompletableFuture<Void> result = loginService.login(loginType, loginParams);

            assertDoesNotThrow(() -> result.get());
            assertEquals("BUILDER_ID", mockPluginStore.get("LOGIN_TYPE"));
            assertNull(mockPluginStore.getObject("IDC_PARAMS", LoginIdcParams.class));
            mockedCredentialUtils.verify(() -> CredentialUtils.getToken(eq(mockLspProvider), any(), eq(loginParams), eq(true)));
            mockedCredentialUtils.verify(() -> CredentialUtils.updateCredentials(mockLspProvider, mockToken));
        }
    }
    @Test
    public void testLoginWithNullTokenThrowsException() {
        resetLoginService();
        LoginType loginType = LoginType.BUILDER_ID;
        LoginParams loginParams = new LoginParams();

        try (MockedStatic<CredentialUtils> mockedCredentialUtils = mockStatic(CredentialUtils.class);
             MockedStatic<Activator> mockedActivator = mockStatic(Activator.class)) {
            LoggingService mockLogger = mockLoggingService(mockedActivator);
            mockedCredentialUtils.when(() -> CredentialUtils.getToken(eq(mockLspProvider), any(), any(), eq(true)))
                    .thenReturn(CompletableFuture.completedFuture(null));
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
            when(mockLspProvider.getAmazonQServer()).thenReturn(CompletableFuture.completedFuture(mockAmazonQServer));
            when(mockAmazonQServer.invalidateSsoToken(any())).thenReturn(CompletableFuture.completedFuture(null));

            CompletableFuture<Void> result = loginService.logout();
            assertDoesNotThrow(() -> result.get());
            mockedCredentialUtils.verify(() -> CredentialUtils.getToken(eq(mockLspProvider), eq(LoginType.BUILDER_ID), any(), eq(false)), times(2));

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
