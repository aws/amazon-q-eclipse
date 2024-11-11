package software.aws.toolkits.eclipse.amazonq.lsp.auth;

import static org.mockito.Mockito.mock;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import software.aws.toolkits.eclipse.amazonq.configuration.DefaultPluginStore;
import software.aws.toolkits.eclipse.amazonq.configuration.PluginStore;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.lsp.AmazonQLspServer;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.GetSsoTokenParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.GetSsoTokenResult;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginIdcParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.Profile;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.ProfileSettings;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.SsoSession;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.SsoToken;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.UpdateProfileOptions;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.UpdateProfileParams;
import software.aws.toolkits.eclipse.amazonq.lsp.encryption.LspEncryptionManager;
import software.aws.toolkits.eclipse.amazonq.providers.LspProvider;
import software.aws.toolkits.eclipse.amazonq.util.Constants;
import software.aws.toolkits.eclipse.amazonq.util.QConstants;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class NewDefaultLoginServiceTest {

    private Method validateLoginParameters;
    private Method getToken;
    private Method processLogin;
    private Method updateCredentials;

    private static DefaultLoginService loginService;
    private static LspProvider mockLspProvider;
    private static LspEncryptionManager mockEncryptionManager;
    private static AmazonQLspServer mockAmazonQServer;
    private static PluginStore mockPluginStore;
    private static GetSsoTokenResult mockSsoTokenResult;

    @BeforeEach
    public final void setUp() {
        mockLspProvider = mock(LspProvider.class);
        mockAmazonQServer = mock(AmazonQLspServer.class);
        mockEncryptionManager = mock(LspEncryptionManager.class);
        mockPluginStore = mock(DefaultPluginStore.class);
        mockSsoTokenResult = mock(GetSsoTokenResult.class);

        resetLoginService();
        
        when(mockLspProvider.getAmazonQServer())
        .thenReturn(CompletableFuture.completedFuture(mockAmazonQServer));
        when(mockAmazonQServer.getSsoToken(any()))
            .thenReturn(CompletableFuture.completedFuture(mockSsoTokenResult));
    }

    @Test
    void processLogin_IDC_WithLoginOnInvalidToken_Success() throws Exception {
        setupAccessToProcessLogin();
        
        LoginType loginType = LoginType.IAM_IDENTITY_CENTER;
        LoginParams loginParams = createValidLoginParams();
        boolean loginOnInvalidToken = true;
        
        invokeProcessLogin(loginType, loginParams, loginOnInvalidToken);
    }
    
//    @Test
//    void processLogin_GetTokenFails_ThrowsException() throws Exception {
//        // Arrange
//        LoginType loginType = LoginType.IAM_IDENTITY_CENTER;
//        LoginParams loginParams = createValidLoginParams();
//        boolean loginOnInvalidToken = false;
//        
//        CompletableFuture<AmazonQServer> failedFuture = CompletableFuture.failedFuture(
//            new IllegalArgumentException("Token fetch failed")
//        );
//        when(mockLspProvider.getAmazonQServer()).thenReturn(failedFuture);
//        
//        // Act
//        CompletableFuture<Void> result = loginService.processLogin(loginType, loginParams, loginOnInvalidToken);
//        
//        // Assert
//        ExecutionException exception = assertThrows(ExecutionException.class, () -> result.get());
//        assertAll(
//            () -> assertTrue(exception.getCause() instanceof AmazonQPluginException),
//            () -> assertEquals("Failed to process log in", exception.getCause().getMessage()),
//            () -> assertTrue(exception.getCause().getCause() instanceof IllegalArgumentException)
//        );
//        
//        verify(authStateManager, never()).toLoggedIn(any(), any(), any());
//        verifyNoMoreInteractions(authStateManager);
//    }
//    
//    @Test
//    void processLogin_UpdateCredentialsFails_ThrowsException() throws Exception {
//        // Arrange
//        LoginType loginType = LoginType.IAM_IDENTITY_CENTER;
//        LoginParams loginParams = createValidLoginParams();
//        boolean loginOnInvalidToken = false;
//        String ssoTokenId = "testTokenId";
//        String accessToken = "testAccessToken";
//        SsoToken mockSsoToken = new SsoToken(ssoTokenId, accessToken);
//        
//        when(mockSsoTokenResult.ssoToken()).thenReturn(mockSsoToken);
//        when(loginService.updateCredentials(any()))
//            .thenReturn(CompletableFuture.failedFuture(
//                new IllegalStateException("Credential update failed")
//            ));
//        
//        // Act
//        CompletableFuture<Void> result = loginService.processLogin(loginType, loginParams, loginOnInvalidToken);
//        
//        // Assert
//        ExecutionException exception = assertThrows(ExecutionException.class, () -> result.get());
//        assertAll(
//            () -> assertTrue(exception.getCause() instanceof AmazonQPluginException),
//            () -> assertEquals("Failed to process log in", exception.getCause().getMessage()),
//            () -> assertTrue(exception.getCause().getCause() instanceof IllegalStateException)
//        );
//        
//        verify(authStateManager, never()).toLoggedIn(any(), any(), any());
//        verify(loginService).updateCredentials(mockSsoToken);
//    }
//    
//    @Test
//    void processLogin_InvalidLoginParameters_ThrowsException() {
//        // Arrange
//        LoginType loginType = null;
//        LoginParams loginParams = createValidLoginParams();
//        boolean loginOnInvalidToken = false;
//        
//        // Act & Assert
//        CompletableFuture<Void> result = loginService.processLogin(loginType, loginParams, loginOnInvalidToken);
//        
//        ExecutionException exception = assertThrows(ExecutionException.class, () -> result.get());
//        assertAll(
//            () -> assertTrue(exception.getCause() instanceof AmazonQPluginException),
//            () -> assertEquals("Failed to process log in", exception.getCause().getMessage()),
//            () -> assertTrue(exception.getCause().getCause() instanceof IllegalArgumentException),
//            () -> assertEquals("Missing required parameter: loginType cannot be null", 
//                exception.getCause().getCause().getMessage())
//        );
//        
//        verify(authStateManager, never()).toLoggedIn(any(), any(), any());
//        verifyNoInteractions(mockAmazonQServer);
//    }
//    
//    @Test
//    void processLogin_AuthStateManagerFails_ThrowsException() throws Exception {
//        // Arrange
//        LoginType loginType = LoginType.IAM_IDENTITY_CENTER;
//        LoginParams loginParams = createValidLoginParams();
//        boolean loginOnInvalidToken = false;
//        String ssoTokenId = "testTokenId";
//        String accessToken = "testAccessToken";
//        
//        when(mockSsoTokenResult.ssoToken()).thenReturn(mockSsoToken);
//        when(loginService.updateCredentials(any()))
//            .thenReturn(CompletableFuture.completedFuture(null));
//        doThrow(new RuntimeException("Auth state update failed"))
//            .when(authStateManager).toLoggedIn(any(), any(), any());
//        
//        // Act
//        CompletableFuture<Void> result = loginService.processLogin(loginType, loginParams, loginOnInvalidToken);
//        
//        // Assert
//        ExecutionException exception = assertThrows(ExecutionException.class, () -> result.get());
//        assertAll(
//            () -> assertTrue(exception.getCause() instanceof AmazonQPluginException),
//            () -> assertEquals("Failed to process log in", exception.getCause().getMessage()),
//            () -> assertTrue(exception.getCause().getCause() instanceof RuntimeException)
//        );
//        
//        verify(authStateManager).toLoggedIn(loginType, loginParams, ssoTokenId);
//        verify(loginService).updateCredentials(mockSsoToken);
//    }
    

    @Test
    void getToken_BuilderId_NoLoginOnInvalidToken_Success() throws Exception {
        LoginType loginType = LoginType.BUILDER_ID;
        LoginParams loginParams = new LoginParams(); // LoginParams is not required for BUILDER_ID
        String ssoTokenId = "ssoTokenId";
        String accessToken = "accessToken";
        SsoToken expectedToken = new SsoToken(ssoTokenId, accessToken);
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
        String ssoTokenId = "ssoTokenId";
        String accessToken = "accessToken";
        SsoToken expectedToken = new SsoToken(ssoTokenId, accessToken);
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
        String ssoTokenId = "ssoTokenId";
        String accessToken = "accessToken";
        SsoToken expectedToken = new SsoToken(ssoTokenId, accessToken);
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
        String ssoTokenId = "ssoTokenId";
        String accessToken = "accessToken";
        SsoToken expectedToken = new SsoToken(ssoTokenId, accessToken);
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
        setupAccessToValidateLoginParameters();
        
        LoginType loginType = null;
        LoginParams loginParams = createValidLoginParams();
        
        try {
            validateLoginParameters.invoke(loginService, loginType, loginParams);
            fail("Expected IllegalArgumentException to be thrown");
        } catch (Exception ex) {
            assertTrue(ex.getCause() instanceof IllegalArgumentException);
            assertEquals("Missing required parameter: loginType cannot be null", 
                ex.getCause().getMessage());
        }
    }
    
    @Test 
    void validateLoginParameters_WithNoneLoginType_ThrowsException() {
        setupAccessToValidateLoginParameters();
        
        LoginType loginType = LoginType.NONE;
        LoginParams loginParams = createValidLoginParams();
        
        try {
            validateLoginParameters.invoke(loginService, loginType, loginParams);
            fail("Expected IllegalArgumentException to be thrown");
        } catch (Exception ex) {
            assertTrue(ex.getCause() instanceof IllegalArgumentException);
            assertEquals("Invalid loginType: NONE is not a valid login type", 
                ex.getCause().getMessage());
        }
    }
    
    @Test 
    void validateLoginParameters_WithNullLoginParams_ThrowsException() {
        setupAccessToValidateLoginParameters();
        
        LoginType loginType = LoginType.BUILDER_ID;
        LoginParams loginParams = null;
        
        try {
            validateLoginParameters.invoke(loginService, loginType, loginParams);
            fail("Expected IllegalArgumentException to be thrown");
        } catch (Exception ex) {
            assertTrue(ex.getCause() instanceof IllegalArgumentException);
            assertEquals("Missing required parameter: loginParams cannot be null", 
                ex.getCause().getMessage());
        }
    }
    
    @Test 
    void validateLoginParameters_WithValidParams_Success() {
        setupAccessToValidateLoginParameters();
        
        LoginType loginType = LoginType.BUILDER_ID;
        LoginParams loginParams = createValidLoginParams();
        
        try {
            validateLoginParameters.invoke(loginService, loginType, loginParams);
        } catch (Exception ex) {
            fail("Expected no exception");
        }
    }
    
    private void resetLoginService() {
      loginService = new DefaultLoginService.Builder()
              .withLspProvider(mockLspProvider)
              .withPluginStore(mockPluginStore)
              .withEncryptionManager(mockEncryptionManager)
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
    
    private void setupAccessToValidateLoginParameters() {
        try {
            validateLoginParameters = DefaultLoginService.class.getDeclaredMethod("validateLoginParameters",
                    LoginType.class, LoginParams.class);
            validateLoginParameters.setAccessible(true);
        } catch (Exception ex) {
            throw new AmazonQPluginException("Failed to provide access to validateLoginParameters");
        }
    }
    
    private void setupAccessToGetToken() {
        try {
            getToken = DefaultLoginService.class.getDeclaredMethod("getToken",
                    LoginType.class, LoginParams.class, boolean.class);
            getToken.setAccessible(true);
        } catch (Exception ex) {
            throw new AmazonQPluginException("Failed to provide access to getToken");
        }
    }
    
    private SsoToken invokeGetToken(LoginType loginType, LoginParams loginParams, boolean loginOnInvalidToken) throws Exception {
        setupAccessToGetToken();
        
        Object getTokenFuture = getToken.invoke(loginService, loginType, loginParams, loginOnInvalidToken);
        assertTrue(getTokenFuture instanceof CompletableFuture<?>, "Return value should be CompletableFuture");

        CompletableFuture<?> future = (CompletableFuture<?>) getTokenFuture;
        Object result = future.get();
        assertTrue(result instanceof SsoToken, "getTokenFuture result should be SsoToken");
        
        return (SsoToken) result;
    }
    
    private void setupAccessToProcessLogin() {
        try {
            processLogin = DefaultLoginService.class.getDeclaredMethod("processLogin",
                    LoginType.class, LoginParams.class, boolean.class);
            processLogin.setAccessible(true);
        } catch (Exception ex) {
            throw new AmazonQPluginException("Failed to provide access to processLogin");
        }
    }
    
    private void invokeProcessLogin(LoginType loginType, LoginParams loginParams, boolean loginOnInvalidToken) throws Exception {
        setupAccessToProcessLogin();
        
        Object getTokenFuture = processLogin.invoke(loginService, loginType, loginParams, loginOnInvalidToken);
        assertTrue(getTokenFuture instanceof CompletableFuture<?>, "Return value should be CompletableFuture");

        CompletableFuture<?> future = (CompletableFuture<?>) getTokenFuture;
        Object result = future.get();
        assertNull(result);
    }
    
    private void setupAccessToUpdateCredentials() {
        try {
            updateCredentials = DefaultLoginService.class.getDeclaredMethod("updateCredentials", SsoToken.class);
            updateCredentials.setAccessible(true);
        } catch (Exception ex) {
            throw new AmazonQPluginException("Failed to provide access to processLogin");
        }
    }
}
