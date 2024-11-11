// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.auth;

import software.aws.toolkits.eclipse.amazonq.configuration.PluginStore;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthStateType;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.AuthUtil;

public final class DefaultAuthStateManager implements AuthStateManager {
    private final AuthPluginStore authPluginStore;

    private AuthStateType authStateType;
    private LoginType loginType;
    private LoginParams loginParams;
    private String issuerUrl;
    private String ssoTokenId;

    public DefaultAuthStateManager(final PluginStore pluginStore) {
        this.authPluginStore = new AuthPluginStore(pluginStore);
        syncAuthStateWithPluginStore();
    }

    @Override
    public void toLoggedIn(final LoginType loginType, final LoginParams loginParams, final String ssoTokenId) {
        if (loginType == null) {
            throw new IllegalArgumentException("Missing required parameter: loginType cannot be null");
        }

        if (loginType.equals(LoginType.NONE)) {
            throw new IllegalArgumentException("Invalid loginType: NONE is not a valid login type");
        }

        if (loginParams == null) {
            throw new IllegalArgumentException("Missing required parameter: loginParams cannot be null");
        }

        if (ssoTokenId == null) {
            throw new IllegalArgumentException("Missing required parameter: ssoTokenId cannot be null");
        }


        updateState(AuthStateType.LOGGED_IN, loginType, loginParams, ssoTokenId);
    }

    @Override
    public void toLoggedOut() {
        updateState(AuthStateType.LOGGED_OUT, LoginType.NONE, null, null);
    }

    @Override
    public void toExpired() {
        if (loginType == null || loginType.equals(LoginType.NONE) || loginParams == null) {
            Activator.getLogger().error("Attempted to switch to expired state but missing required parameteres for"
                    + " re-authentication. Switching to logged out state instead.");
            toLoggedOut();
            return;
        }

        updateState(AuthStateType.EXPIRED, loginType, loginParams, ssoTokenId);
    }

    @Override
    public AuthState getAuthState() {
        return new AuthState(authStateType, loginType, loginParams, issuerUrl, ssoTokenId);
    }

    private void updateState(final AuthStateType authStatusType, final LoginType loginType, final LoginParams loginParams, final String ssoTokenId) {
        this.authStateType = authStatusType;
        this.loginType = loginType;
        this.loginParams = loginParams;
        this.issuerUrl = AuthUtil.getIssuerUrl(loginType, loginParams);
        this.ssoTokenId = ssoTokenId;

        if (loginType.equals(LoginType.NONE)) {
            authPluginStore.clear();
        } else {
            authPluginStore.setLoginType(loginType);
            authPluginStore.setLoginIdcParams(loginParams);
            authPluginStore.setSsoTokenId(ssoTokenId);
        }

        AuthStatusProvider.notifyAuthStatusChanged(getAuthState());
    }

    private void syncAuthStateWithPluginStore() {
        LoginType loginType = authPluginStore.getLoginType();
        LoginParams loginParams = authPluginStore.getLoginIdcParams();
        String ssoTokenId = authPluginStore.getSsoTokenId();

        if (loginType.equals(LoginType.NONE)) {
            toLoggedOut();
            return;
        }

        updateState(AuthStateType.LOGGED_IN, loginType, loginParams, ssoTokenId);
    }
}
