// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import java.util.Objects;

import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;
import software.aws.toolkits.eclipse.amazonq.views.model.AuthState;
import software.aws.toolkits.eclipse.amazonq.views.model.AuthStateType;

public final class AuthStateManager {
    private final AuthPluginStore authPluginStore;

    private AuthStateType authStateType;
    private LoginType loginType;
    private LoginParams loginParams;
    private String issuerUrl;

    public AuthStateManager() {
        this.authPluginStore = new AuthPluginStore();
        syncAuthStateWithPluginStore();
    }

    public void toLoggedIn(final LoginType loginType, final LoginParams loginParams) {
        updateState(AuthStateType.LOGGED_IN, loginType, loginParams);
        authPluginStore.setLoginType(loginType);
        authPluginStore.setLoginParams(loginParams);
    }

    public void toLoggedOut() {
        updateState(AuthStateType.LOGGED_OUT, LoginType.NONE, null);
        authPluginStore.setLoginType(loginType);
        authPluginStore.setLoginParams(loginParams);
    }

    public void toExpired() {
        updateState(AuthStateType.LOGGED_OUT, LoginType.NONE, null);
        authStateType = AuthStateType.EXPIRED;
    }

    public void toExpired(final LoginType loginType, final LoginParams loginParams) {
        this.authStateType = AuthStateType.EXPIRED;
        this.loginType = loginType;
        this.loginParams = loginParams;
        this.issuerUrl = getIssuerUrl(loginType, loginParams);

        authPluginStore.setLoginType(loginType);
        authPluginStore.setLoginParams(loginParams);
    }

    public AuthStateType getAuthStateType() {
        return authStateType;
    }

    public LoginType getLoginType() {
        return loginType;
    }

    public LoginParams getLoginParams() {
        return loginParams;
    }

    public String getIssuerUrl() {
        return issuerUrl;
    }

    public AuthState getAuthState() {
        return new AuthState(authStateType, loginType, loginParams, issuerUrl);
    }

    private void updateState(final AuthStateType authStatusType, final LoginType loginType, final LoginParams loginParams) {
        this.authStateType = authStatusType;
        this.loginType = loginType;
        this.loginParams = loginParams;
        this.issuerUrl = getIssuerUrl(loginType, loginParams);

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
        LoginParams loginParams = authPluginStore.getLoginParams();

        if (loginType.equals(LoginType.NONE)) {
            toLoggedOut();
            return;
        }

        // Default to expired. We have the loginType and params therefore we know the user
        // has previously authenticated. It would be unsafe to move to a LOGGED-IN state.
        toExpired(loginType, loginParams);
    }
}
