// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import software.aws.toolkits.eclipse.amazonq.configuration.PluginStore;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginIdcParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;
import software.aws.toolkits.eclipse.amazonq.util.Constants;

public final class AuthPluginStoreTest {

    @Mock
    private PluginStore pluginStore;

    private AuthPluginStore authPluginStore;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        authPluginStore = new AuthPluginStore(pluginStore);
    }

    @Test
    public void testSetLoginType() {
        authPluginStore.setLoginType(LoginType.BUILDER_ID);

        verify(pluginStore).put(Constants.LOGIN_TYPE_KEY, LoginType.BUILDER_ID.name());
    }

    @Test
    public void testGetLoginTypeWhenNullReturnsNone() {
        when(pluginStore.get(Constants.LOGIN_TYPE_KEY)).thenReturn(null);

        LoginType result = authPluginStore.getLoginType();

        assertEquals(LoginType.NONE, result);
    }

    @Test
    public void testGetLoginTypeWhenBuilderIdReturnsBuilderId() {
        when(pluginStore.get(Constants.LOGIN_TYPE_KEY)).thenReturn(LoginType.BUILDER_ID.name());

        LoginType result = authPluginStore.getLoginType();

        assertEquals(LoginType.BUILDER_ID, result);
    }

    @Test
    public void testGetLoginTypeWhenIamIdentityCenterReturnsIamIdentityCenter() {
        when(pluginStore.get(Constants.LOGIN_TYPE_KEY)).thenReturn(LoginType.IAM_IDENTITY_CENTER.name());

        LoginType result = authPluginStore.getLoginType();

        assertEquals(LoginType.IAM_IDENTITY_CENTER, result);
    }

    @Test
    public void testGetLoginTypeWhenInvalidReturnsNone() {
        when(pluginStore.get(Constants.LOGIN_TYPE_KEY)).thenReturn("INVALID_TYPE");

        LoginType result = authPluginStore.getLoginType();

        assertEquals(LoginType.NONE, result);
    }

    @Test
    public void testSetLoginIdcParams() {
        LoginIdcParams idcParams = new LoginIdcParams();
        LoginParams loginParams = new LoginParams();
        loginParams.setLoginIdcParams(idcParams);

        authPluginStore.setLoginIdcParams(loginParams);

        verify(pluginStore).putObject(Constants.LOGIN_IDC_PARAMS_KEY, idcParams);
    }

    @Test
    public void testGetLoginIdcParams() {
        LoginIdcParams idcParams = new LoginIdcParams();
        when(pluginStore.getObject(Constants.LOGIN_IDC_PARAMS_KEY, LoginIdcParams.class)).thenReturn(idcParams);

        LoginParams result = authPluginStore.getLoginIdcParams();

        assertNotNull(result);
        assertEquals(idcParams, result.getLoginIdcParams());
    }

    @Test
    public void testSetSsoTokenId() {
        String tokenId = "test-token-id";

        authPluginStore.setSsoTokenId(tokenId);

        verify(pluginStore).put(Constants.SSO_TOKEN_ID, tokenId);
    }

    @Test
    public void testGetSsoTokenId() {
        String expectedToken = "test-token-id";
        when(pluginStore.get(Constants.SSO_TOKEN_ID)).thenReturn(expectedToken);

        String result = authPluginStore.getSsoTokenId();

        assertEquals(expectedToken, result);
    }

    @Test
    public void testClear() {
        authPluginStore.clear();

        verify(pluginStore).remove(Constants.LOGIN_TYPE_KEY);
        verify(pluginStore).remove(Constants.LOGIN_IDC_PARAMS_KEY);
        verify(pluginStore).remove(Constants.SSO_TOKEN_ID);
    }
}
