// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

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
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.providers.LspProvider;
import software.aws.toolkits.eclipse.amazonq.util.Constants;
import software.aws.toolkits.eclipse.amazonq.util.LoggingService;
import software.aws.toolkits.eclipse.amazonq.util.QConstants;

public final class DefaultAuthTokenServiceTest {
    private static DefaultAuthTokenService authTokenService;
    private static LspProvider mockLspProvider;
    private static AmazonQLspServer mockAmazonQServer;
    private static GetSsoTokenResult mockSsoTokenResult;
    private static MockedStatic<Activator> mockedActivator;
    private static LoggingService mockLoggingService;

    @BeforeEach
    public void setUp() {
        mockLspProvider = mock(LspProvider.class);
        mockAmazonQServer = mock(AmazonQLspServer.class);
        mockSsoTokenResult = mock(GetSsoTokenResult.class);
        mockLoggingService = mock(LoggingService.class);
        mockedActivator = mockStatic(Activator.class);
        mockedActivator.when(Activator::getLogger).thenReturn(mockLoggingService);

        resetAuthTokenService();

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
    void getSsoTokenBuilderIdNoLoginOnInvalidTokenSuccess() throws Exception {
        LoginType loginType = LoginType.BUILDER_ID;
        LoginParams loginParams = new LoginParams(); // LoginParams is not required for BUILDER_ID
        SsoToken expectedToken = createSsoToken();
        when(mockSsoTokenResult.ssoToken()).thenReturn(expectedToken);
        boolean loginOnInvalidToken = false;

        SsoToken actualToken = invokeGetSsoToken(loginType, loginParams, loginOnInvalidToken);

        assertEquals(expectedToken.id(), actualToken.id());
        assertEquals(expectedToken.accessToken(), actualToken.accessToken());
        verify(mockAmazonQServer).getSsoToken(any(GetSsoTokenParams.class));
        verifyNoMoreInteractions(mockAmazonQServer);
    }

    @Test
    void getSsoTokenBuilderIdWithLoginOnInvalidTokenSuccess() throws Exception {
        LoginType loginType = LoginType.BUILDER_ID;
        LoginParams loginParams = new LoginParams(); // LoginParams is not required for BUILDER_ID
        SsoToken expectedToken = createSsoToken();
        when(mockSsoTokenResult.ssoToken()).thenReturn(expectedToken);
        boolean loginOnInvalidToken = true;

        SsoToken actualToken = invokeGetSsoToken(loginType, loginParams, loginOnInvalidToken);

        assertEquals(expectedToken.id(), actualToken.id());
        assertEquals(expectedToken.accessToken(), actualToken.accessToken());
        verify(mockAmazonQServer).getSsoToken(any(GetSsoTokenParams.class));
        verifyNoMoreInteractions(mockAmazonQServer);
    }

    @Test
    void getSsoTokenIDCNoLoginOnInvalidTokenSuccess() throws Exception {
        LoginType loginType = LoginType.IAM_IDENTITY_CENTER;
        LoginParams loginParams = createValidLoginParams();
        SsoToken expectedToken = createSsoToken();
        when(mockSsoTokenResult.ssoToken()).thenReturn(expectedToken);
        boolean loginOnInvalidToken = false;

        SsoToken actualToken = invokeGetSsoToken(loginType, loginParams, loginOnInvalidToken);

        assertEquals(expectedToken.id(), actualToken.id());
        assertEquals(expectedToken.accessToken(), actualToken.accessToken());
        verify(mockAmazonQServer).getSsoToken(any(GetSsoTokenParams.class));
        verifyNoMoreInteractions(mockAmazonQServer);
    }

    @Test
    void getSsoTokenIDCWithLoginOnInvalidTokenSuccess() throws Exception {
        ArgumentCaptor<UpdateProfileParams> updateProfileParamsCaptor = ArgumentCaptor.forClass(UpdateProfileParams.class);
        when(mockAmazonQServer.updateProfile(any()))
            .thenReturn(CompletableFuture.completedFuture(null));

        LoginType loginType = LoginType.IAM_IDENTITY_CENTER;
        LoginParams loginParams = createValidLoginParams();
        SsoToken expectedToken = createSsoToken();
        when(mockSsoTokenResult.ssoToken()).thenReturn(expectedToken);
        boolean loginOnInvalidToken = true;

        SsoToken actualToken = invokeGetSsoToken(loginType, loginParams, loginOnInvalidToken);

        assertEquals(expectedToken.id(), actualToken.id());
        assertEquals(expectedToken.accessToken(), actualToken.accessToken());
        verify(mockAmazonQServer).updateProfile(updateProfileParamsCaptor.capture());
        UpdateProfileParams actualParams = updateProfileParamsCaptor.getValue();
        verifyUpdateProfileParams(actualParams);
        verify(mockAmazonQServer).getSsoToken(any(GetSsoTokenParams.class));
        verifyNoMoreInteractions(mockAmazonQServer);
    }

    private SsoToken invokeGetSsoToken(final LoginType loginType, final LoginParams loginParams, final boolean loginOnInvalidToken) throws Exception {
        Object getSsoTokenFuture = authTokenService.getSsoToken(loginType, loginParams, loginOnInvalidToken);
        assertTrue(getSsoTokenFuture instanceof CompletableFuture<?>, "Return value should be CompletableFuture");

        CompletableFuture<?> future = (CompletableFuture<?>) getSsoTokenFuture;
        Object result = future.get();
        assertTrue(result instanceof SsoToken, "getSsoTokenFuture result should be SsoToken");

        return (SsoToken) result;
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

    private void resetAuthTokenService() {
        authTokenService = DefaultAuthTokenService.builder()
                .withLspProvider(mockLspProvider)
                .build();
        authTokenService = spy(authTokenService);
      }
}
