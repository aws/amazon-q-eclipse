// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.MockedStatic;

import software.amazon.awssdk.services.toolkittelemetry.model.AWSProduct;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.concurrent.CompletableFuture;
import software.aws.toolkits.eclipse.amazonq.providers.LspProvider;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.lsp.AuthLspServer;
import software.aws.toolkits.eclipse.amazonq.lsp.AmazonQLspServer;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginIdcParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.SsoToken;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.GetSsoTokenResult;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseMessage;
import software.aws.toolkits.eclipse.amazonq.lsp.model.UpdateCredentialsPayload;
import software.aws.toolkits.eclipse.amazonq.lsp.model.BearerCredentials;

public class CredentialUtilsTest {

    @Mock
    private LspProvider mockLspProvider;

    @Mock
    private AuthLspServer mockAuthServer;

    @Mock
    private AmazonQLspServer mockAmazonQServer;

    @Mock
    private SsoToken mockSsoToken;

    private static MockedStatic<Activator> mockedActivator;
    private static LoggingService mockLogger;

    @BeforeEach
    final void setUpMocks() {
        MockitoAnnotations.openMocks(this);
    }

    @BeforeAll
    static final void setUpMocksBeforeAll() {
        mockLogger = mock(LoggingService.class);
        mockedActivator = mockStatic(Activator.class);
        mockedActivator.when(() -> Activator.getLogger()).thenReturn(mockLogger);
        doNothing().when(mockLogger).error(anyString(), any(Exception.class));
    }

    @AfterAll
    static final void tearDownActivator() {
        mockedActivator.close();
    }

    @Test
    public void testGetTokenWithIdcAndTriggerSignIn() {
        LoginType currentLogin = LoginType.IAM_IDENTITY_CENTER;
        LoginParams loginParams = mock(LoginParams.class);
        LoginIdcParams idcParams = mock(LoginIdcParams.class);
        when(loginParams.getLoginIdcParams()).thenReturn(idcParams);
        when(idcParams.getUrl()).thenReturn("testUrl");
        when(idcParams.getRegion()).thenReturn("testRegion");

        SsoToken expectedToken = new SsoToken("id", "accessToken");
        GetSsoTokenResult mockSsoTokenResult = mock(GetSsoTokenResult.class);
        when(mockSsoTokenResult.ssoToken()).thenReturn(expectedToken);

        when(mockLspProvider.getAuthServer()).thenReturn(CompletableFuture.completedFuture(mockAuthServer));
        when(mockAuthServer.getSsoToken(any())).thenReturn(CompletableFuture.completedFuture(mockSsoTokenResult));

        try (MockedStatic<AuthStatusProvider> mockedAuthStatusProvider = mockStatic(AuthStatusProvider.class)) {
            mockedAuthStatusProvider.when(() -> AuthStatusProvider.notifyAuthStatusChanged(any()))
                .thenAnswer(invocation -> {
                    return null;
            });
            CompletableFuture<SsoToken> result = CredentialUtils.getToken(mockLspProvider, currentLogin, loginParams, true);

            SsoToken actualToken = assertDoesNotThrow(() -> result.get());
            assertEquals(expectedToken, actualToken);

            verify(mockAuthServer).getSsoToken(argThat(params ->
                params.source().clientName().equals(AWSProduct.AMAZON_Q_FOR_ECLIPSE.toString())
                && params.source().kind().equals("IamIdentityCenter")
                && params.source().issuerUrl().equals("testUrl")
                && params.source().region().equals("testRegion")
            ));
            verify(mockLspProvider).getAuthServer();
            verify(mockAuthServer).getSsoToken(any());

             mockedAuthStatusProvider.verify(() ->
                 AuthStatusProvider.notifyAuthStatusChanged(argThat(details ->
                 details.getIsLoggedIn() && details.getLoginType() == currentLogin
                 ))
             );
        }
    }

    @Test
    public void testGetTokenWithBuilderId() {
        LoginType currentLogin = LoginType.BUILDER_ID;
        LoginParams loginParams = mock(LoginParams.class);

        SsoToken expectedToken = new SsoToken("id", "accessToken");
        GetSsoTokenResult mockSsoTokenResult = mock(GetSsoTokenResult.class);
        when(mockSsoTokenResult.ssoToken()).thenReturn(expectedToken);

        when(mockLspProvider.getAuthServer()).thenReturn(CompletableFuture.completedFuture(mockAuthServer));
        when(mockAuthServer.getSsoToken(any())).thenReturn(CompletableFuture.completedFuture(mockSsoTokenResult));

        CompletableFuture<SsoToken> result = CredentialUtils.getToken(mockLspProvider, currentLogin, loginParams, false);

        SsoToken actualToken = assertDoesNotThrow(() -> result.get());
        assertEquals(expectedToken, actualToken);

        verify(mockAuthServer).getSsoToken(argThat(params ->
            params.source().clientName().equals(AWSProduct.AMAZON_Q_FOR_ECLIPSE.toString())
            && params.source().kind().equals("AwsBuilderId")
            && params.source().issuerUrl() == null
            && params.source().region() == null
        ));

        try (MockedStatic<AuthStatusProvider> mockedAuthStatusProvider = mockStatic(AuthStatusProvider.class)) {
            mockedAuthStatusProvider.verifyNoInteractions();
        }
    }

    @Test
    public void testGetTokenWithException() {
        LoginType currentLogin = LoginType.BUILDER_ID;
        LoginParams loginParams = mock(LoginParams.class);

        RuntimeException expectedException = new RuntimeException("Test exception");
        when(mockLspProvider.getAuthServer()).thenReturn(CompletableFuture.failedFuture(expectedException));

        CompletableFuture<SsoToken> result = CredentialUtils.getToken(mockLspProvider, currentLogin, loginParams, false);

        Exception exception = assertThrows(Exception.class, () -> result.get());
        assertTrue(exception.getCause() instanceof AmazonQPluginException);

        verify(mockLogger).error(eq("Failed to fetch SSO token from LSP"), any(Throwable.class));
    }

    @Test
    public void testUpdateCredentialsSuccess() {
        updateCredentialsTestHelper(false);
        CompletableFuture<ResponseMessage> result = CredentialUtils.updateCredentials(mockLspProvider, mockSsoToken);

        assertNotNull(result);
        assertDoesNotThrow(() -> result.get());

        verify(mockSsoToken).accessToken();
        verify(mockLspProvider).getAmazonQServer();
        verify(mockAmazonQServer).updateTokenCredentials(argThat(payload ->
        payload.getData() instanceof BearerCredentials
        && ((BearerCredentials) payload.getData()).getToken().equals("expectedAccessToken")
        && !payload.isEncrypted()
    ));
    }

    @Test
    public void testUpdateCredentialsWithException() {
        updateCredentialsTestHelper(true);
        CompletableFuture<ResponseMessage> result = CredentialUtils.updateCredentials(mockLspProvider, mockSsoToken);

        assertNotNull(result);
        Exception exception = assertThrows(Exception.class, () -> result.get());
        assertTrue(exception.getCause() instanceof AmazonQPluginException);

        verify(mockLogger).error(eq("Failed to update credentials with AmazonQ server"), any(RuntimeException.class));
    }

    public final void updateCredentialsTestHelper(final boolean throwsException) {
        when(mockSsoToken.accessToken()).thenReturn("expectedAccessToken");
        when(mockLspProvider.getAmazonQServer()).thenReturn(CompletableFuture.completedFuture(mockAmazonQServer));
        if (throwsException) {
            when(mockAmazonQServer.updateTokenCredentials(any(UpdateCredentialsPayload.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Test exception")));
        } else {
            when(mockAmazonQServer.updateTokenCredentials(any(UpdateCredentialsPayload.class)))
                .thenReturn(CompletableFuture.completedFuture(new ResponseMessage()));
        }
    }
}
