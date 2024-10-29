// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.auth.model;

public class LoginDetails {
    private LoginType loginType;
    private boolean isLoggedIn;
    private String issuerUrl;

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

    public final void setIssuerUrl(final String issuerUrl) {
        this.issuerUrl = issuerUrl;
    }

    public final String getIssuerUrl() {
        return this.issuerUrl;
    }
}
