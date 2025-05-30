// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.auth.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthState(
        @JsonProperty("authStateType") AuthStateType authStateType,
        @JsonProperty("loginType") LoginType loginType,
        @JsonProperty("loginParams") LoginParams loginParams,
        @JsonProperty("issuerUrl") String issuerUrl,
        @JsonProperty("ssoTokenId") String ssoTokenId
    ) {

    public AuthState(final AuthStateType authStateType, final LoginType loginType) {
        this(authStateType, loginType, null, null, null);
    }

    public Boolean isLoggedIn() {
        return authStateType.equals(AuthStateType.LOGGED_IN);
    }

    public Boolean isLoggedOut() {
        return authStateType.equals(AuthStateType.LOGGED_OUT);
    }

    public Boolean isExpired() {
        return authStateType.equals(AuthStateType.EXPIRED);
    }
}
