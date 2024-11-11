// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.auth;

import software.aws.toolkits.eclipse.amazonq.configuration.PluginStore;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginIdcParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;
import software.aws.toolkits.eclipse.amazonq.util.Constants;

public final class AuthPluginStore {

    private PluginStore pluginStore;

    public AuthPluginStore(final PluginStore pluginStore) {
        this.pluginStore = pluginStore;
    }

    public void setLoginType(final LoginType loginType) {
        pluginStore.put(Constants.LOGIN_TYPE_KEY, loginType.name());
    }

    public LoginType getLoginType() {
        String storedValue = pluginStore.get(Constants.LOGIN_TYPE_KEY);

        if (storedValue == null) {
            return LoginType.NONE;
        } else if (storedValue.equals(LoginType.BUILDER_ID.name())) {
            return LoginType.BUILDER_ID;
        } else if (storedValue.equals(LoginType.IAM_IDENTITY_CENTER.name())) {
            return LoginType.IAM_IDENTITY_CENTER;
        } else {
            return LoginType.NONE;
        }
    }

    public void setLoginIdcParams(final LoginParams loginParams) {
        pluginStore.putObject(Constants.LOGIN_IDC_PARAMS_KEY, loginParams.getLoginIdcParams());
    }

    public LoginParams getLoginIdcParams() {
        LoginIdcParams loginIdcParams = pluginStore.getObject(Constants.LOGIN_IDC_PARAMS_KEY, LoginIdcParams.class);
        LoginParams loginParams = new LoginParams();
        loginParams.setLoginIdcParams(loginIdcParams);
        return loginParams;
    }

    public void setSsoTokenId(final String ssoTokenId) {
        pluginStore.put(Constants.SSO_TOKEN_ID, ssoTokenId);
    }

    public String getSsoTokenId() {
        return pluginStore.get(Constants.SSO_TOKEN_ID);
    }

    public void clear() {
        pluginStore.remove(Constants.LOGIN_TYPE_KEY);
        pluginStore.remove(Constants.LOGIN_IDC_PARAMS_KEY);
        pluginStore.remove(Constants.SSO_TOKEN_ID);
    }
}
