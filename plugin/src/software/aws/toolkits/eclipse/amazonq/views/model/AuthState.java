// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginDetails;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;

public record AuthState(
        @JsonProperty("authStateType") AuthStateType authStateType,
        @JsonProperty("loginType") LoginType loginType,
        @JsonProperty("loginParams") LoginParams loginParams,
        @JsonProperty("issuerUrl") String issuerUrl
    ) {

    public Boolean isLoggedIn() {
        return authStateType.equals(AuthStateType.LOGGED_IN);
    }

    public Boolean isLoggedOut() {
        return authStateType.equals(AuthStateType.LOGGED_OUT);
    }

    public Boolean isExpired() {
        return authStateType.equals(AuthStateType.EXPIRED);
    }

    // TODO LoginDetails should be replaced with AuthState throughout the plugin.
    // When AuthStatusProvider and AuthStatusChangedListener is refactored to use
    // AuthState, remove this method as well.
    public LoginDetails toLoginDetails() {
        LoginDetails loginDetails = new LoginDetails();
        loginDetails.setIsLoggedIn(isLoggedIn());
        loginDetails.setIssuerUrl(issuerUrl);
        loginDetails.setLoginType(loginType);
        return loginDetails;
    }
}
