// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import software.aws.toolkits.eclipse.amazonq.configuration.PluginStore;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthStateType;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginIdcParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.Constants;
import software.aws.toolkits.eclipse.amazonq.util.LoggingService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DefaultAuthStateManagerTest {

    @Mock
    private PluginStore pluginStore;
    private MockedStatic<Activator> mockedActivator;
    
    private DefaultAuthStateManager authStateManager;
    private LoginParams loginParams;
    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        
        assertNotNull(pluginStore, "PluginStore mock should not be null");
        mockedActivator = mockStatic(Activator.class);
        mockedActivator.when(Activator::getLogger).thenReturn(mock(LoggingService.class));

        authStateManager = new DefaultAuthStateManager(pluginStore);
        loginParams = new LoginParams();
        LoginIdcParams idcParams = new LoginIdcParams();
        idcParams.setUrl("https://example.com");
        loginParams.setLoginIdcParams(idcParams);
    }

    @AfterEach
    void tearDown() throws Exception {
        clearInvocations(pluginStore);
        mockedActivator.close();
        closeable.close();
    }

    @Test
    void toLoggedIn_WithValidParameters_UpdatesStateCorrectly() {
        String ssoTokenId = "testToken";
        
        authStateManager.toLoggedIn(LoginType.BUILDER_ID, loginParams, ssoTokenId);
        
        // Verify auth state
        AuthState state = authStateManager.getAuthState();
        assertEquals(AuthStateType.LOGGED_IN, state.authStateType());
        assertEquals(LoginType.BUILDER_ID, state.loginType());
        assertEquals(loginParams, state.loginParams());
        assertEquals(ssoTokenId, state.ssoTokenId());
        assertEquals(Constants.AWS_BUILDER_ID_URL, state.issuerUrl());
        
        // Verify plugin store
        verify(pluginStore).put(Constants.LOGIN_TYPE_KEY, LoginType.BUILDER_ID.name());
        verify(pluginStore).putObject(Constants.LOGIN_IDC_PARAMS_KEY, loginParams.getLoginIdcParams());
        verify(pluginStore).put(Constants.SSO_TOKEN_ID, ssoTokenId);
    }

    @Test
    void toLoggedIn_WithNullLoginType_ThrowsException() {
        String ssoTokenId = "testToken";
        
        assertThrows(IllegalArgumentException.class, 
            () -> authStateManager.toLoggedIn(null, loginParams, ssoTokenId));
    }

    @Test
    void toLoggedIn_WithNoneLoginType_ThrowsException() {
        String ssoTokenId = "testToken";
        
        assertThrows(IllegalArgumentException.class, 
            () -> authStateManager.toLoggedIn(LoginType.NONE, loginParams, ssoTokenId));
    }
    
    @Test
    void toLoggedIn_WithNullLoginParams_ThrowsException() {
        String ssoTokenId = "testToken";
        loginParams = null;
        
        assertThrows(IllegalArgumentException.class, 
            () -> authStateManager.toLoggedIn(LoginType.BUILDER_ID, loginParams, ssoTokenId));
    }
    
    @Test
    void toLoggedIn_WithNullSsoTokenId_ThrowsException() {
        String ssoTokenId = null;
        
        assertThrows(IllegalArgumentException.class, 
            () -> authStateManager.toLoggedIn(LoginType.BUILDER_ID, loginParams, ssoTokenId));
    }

    @Test
    void toLoggedOut_ClearsStateCorrectly() {
        String ssoTokenId = "testToken";
        authStateManager.toLoggedIn(LoginType.BUILDER_ID, loginParams, ssoTokenId);
        
        clearInvocations(pluginStore);
        
        authStateManager.toLoggedOut();
        
        // Verify auth state
        AuthState state = authStateManager.getAuthState();
        assertEquals(AuthStateType.LOGGED_OUT, state.authStateType());
        assertEquals(LoginType.NONE, state.loginType());
        assertNull(state.loginParams());
        assertNull(state.ssoTokenId());
        assertNull(state.issuerUrl());
        
        verify(pluginStore).remove(Constants.LOGIN_TYPE_KEY);
        verify(pluginStore).remove(Constants.LOGIN_IDC_PARAMS_KEY);
        verify(pluginStore).remove(Constants.SSO_TOKEN_ID);
    }

    @Test
    void toExpired_UpdatesStateCorrectly() {
        String ssoTokenId = "testToken";
        authStateManager.toLoggedIn(LoginType.BUILDER_ID, loginParams, ssoTokenId);

        clearInvocations(pluginStore);
        
        authStateManager.toExpired();
        
        // Verify auth state
        AuthState state = authStateManager.getAuthState();
        assertEquals(AuthStateType.EXPIRED, state.authStateType());
        assertEquals(LoginType.BUILDER_ID, state.loginType());
        assertEquals(loginParams, state.loginParams());
        assertEquals(ssoTokenId, state.ssoTokenId());
        assertEquals(Constants.AWS_BUILDER_ID_URL, state.issuerUrl());
        
        // Verify plugin store
        verify(pluginStore).put(Constants.LOGIN_TYPE_KEY, LoginType.BUILDER_ID.name());
        verify(pluginStore).putObject(Constants.LOGIN_IDC_PARAMS_KEY, loginParams.getLoginIdcParams());
        verify(pluginStore).put(Constants.SSO_TOKEN_ID, ssoTokenId);
    }

    @Test
    void toExpired_WithNoLoginType_SwitchesToLoggedOut() {
        authStateManager.toLoggedOut();
        
        authStateManager.toExpired();
        
        AuthState state = authStateManager.getAuthState();
        assertEquals(AuthStateType.LOGGED_OUT, state.authStateType());
        assertEquals(LoginType.NONE, state.loginType());
        assertNull(state.loginParams());
        assertNull(state.ssoTokenId());
        assertNull(state.issuerUrl());
    }

    @Test
    void syncAuthStateWithPluginStore_WithStoredCredentials_RestoresState() {
        String ssoTokenId = "testToken";
        
        when(pluginStore.get(Constants.LOGIN_TYPE_KEY)).thenReturn(LoginType.BUILDER_ID.name());
        when(pluginStore.getObject(Constants.LOGIN_IDC_PARAMS_KEY, LoginIdcParams.class)).thenReturn(loginParams.getLoginIdcParams());
        when(pluginStore.get(Constants.SSO_TOKEN_ID)).thenReturn(ssoTokenId);
        
        DefaultAuthStateManager newManager = new DefaultAuthStateManager(pluginStore);
        
        // Verify auth state
        AuthState state = newManager.getAuthState();
        assertEquals(AuthStateType.EXPIRED, state.authStateType());
        assertEquals(LoginType.BUILDER_ID, state.loginType());
        assertEquals(loginParams.getLoginIdcParams().getUrl(), state.loginParams().getLoginIdcParams().getUrl());
        assertEquals(ssoTokenId, state.ssoTokenId());
        assertEquals(Constants.AWS_BUILDER_ID_URL, state.issuerUrl());
    }

    @Test
    void syncAuthStateWithPluginStore_WithNoStoredCredentials_SetsLoggedOut() {
        when(pluginStore.get(Constants.LOGIN_TYPE_KEY)).thenReturn(LoginType.NONE.name());
        
        DefaultAuthStateManager newManager = new DefaultAuthStateManager(pluginStore);
        
        // Verify auth state
        AuthState state = newManager.getAuthState();
        assertEquals(AuthStateType.LOGGED_OUT, state.authStateType());
        assertEquals(LoginType.NONE, state.loginType());
        assertNull(state.loginParams());
        assertNull(state.ssoTokenId());
        assertNull(state.issuerUrl());
    }
}