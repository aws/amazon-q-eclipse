// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.auth.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LoginDetails {
    @JsonProperty("loginType")
    private LoginType loginType;

    @JsonProperty("isLoggedIn")
    private boolean isLoggedIn;

    public final void setLoginType(final LoginType loginType) {
        this.loginType = loginType;
    }

    public final LoginType getLoginType() {
        return this.loginType;
    }

    public final void setIsLoggedIn(final boolean isLoggedIn) {
        this.isLoggedIn = isLoggedIn;
    }

    public final boolean getIsLoggedIn() {
        return this.isLoggedIn;
    }
}
