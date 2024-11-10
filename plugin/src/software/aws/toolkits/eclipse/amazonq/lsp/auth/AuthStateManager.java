// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.auth;

import java.util.Objects;

import software.aws.toolkits.eclipse.amazonq.configuration.PluginStore;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthStateType;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.AuthPluginStore;
import software.aws.toolkits.eclipse.amazonq.util.AuthStatusProvider;
import software.aws.toolkits.eclipse.amazonq.util.Constants;

public final class AuthStateManager {
    private final AuthPluginStore authPluginStore;

    private AuthStateType authStateType;
    private LoginType loginType;
    private LoginParams loginParams;
    private String issuerUrl;
    private String ssoTokenId;

    public AuthStateManager(final PluginStore pluginStore) {
        this.authPluginStore = new AuthPluginStore(pluginStore);
        syncAuthStateWithPluginStore();
    }

    public void toLoggedIn(final LoginType loginType, final LoginParams loginParams, final String ssoTokenId) {
        if (loginType == null || loginType.equals(LoginType.NONE)) {
            Activator.getLogger().error("Error occurred while switching to logged in auth state: "
                    + "Missing required loginType parameter.");
            return;
        }

        updateState(AuthStateType.LOGGED_IN, loginType, loginParams, ssoTokenId);
    }

    public void toLoggedOut() {
        updateState(AuthStateType.LOGGED_OUT, LoginType.NONE, null, null);
    }

    public void toExpired() {
        if (loginType == null || loginType.equals(LoginType.NONE)) {
            Activator.getLogger().error("Error occurred while switching to expired auth state: "
                    + "Missing required loginType parameter. Switching to logged out state instead.");
            toLoggedOut();
            return;
        }

        updateState(AuthStateType.EXPIRED, loginType, loginParams, ssoTokenId);
    }

    public AuthState getAuthState() {
        return new AuthState(authStateType, loginType, loginParams, issuerUrl, ssoTokenId);
    }

    private void updateState(final AuthStateType authStatusType, final LoginType loginType, final LoginParams loginParams, final String ssoTokenId) {
        this.authStateType = authStatusType;
        this.loginType = loginType;
        this.loginParams = loginParams;
        this.issuerUrl = getIssuerUrl(loginType, loginParams);
        this.ssoTokenId = ssoTokenId;

        if (loginType.equals(LoginType.NONE)) {
            authPluginStore.clear();
        } else {
            authPluginStore.setLoginType(loginType);
            authPluginStore.setLoginIdcParams(loginParams);
        }

        AuthStatusProvider.notifyAuthStatusChanged(getAuthState());
    }

    private String getIssuerUrl(final LoginType loginType, final LoginParams loginParams) {
        if (loginType.equals(LoginType.NONE)) {
            return null;
        }
        if (loginType.equals(LoginType.BUILDER_ID)) {
            return Constants.AWS_BUILDER_ID_URL;
        }

        if (Objects.isNull(loginParams) || Objects.isNull(loginParams.getLoginIdcParams())) {
            return null;
        }

        return loginParams.getLoginIdcParams().getUrl();
    }

    private void syncAuthStateWithPluginStore() {
        LoginType loginType = authPluginStore.getLoginType();
        LoginParams loginParams = authPluginStore.getLoginIdcParams();

        if (loginType.equals(LoginType.NONE)) {
            toLoggedOut();
            return;
        }

        // Default to expired. We have the loginType and params therefore we know the user
        // has previously authenticated. It would be unsafe to move to a logged in state.
        updateState(AuthStateType.EXPIRED, loginType, loginParams, ssoTokenId);
    }
}
