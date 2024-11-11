// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.auth.model;

public class LoginParams {
    private LoginIdcParams loginIdcParams;

    public final LoginIdcParams getLoginIdcParams() {
        return loginIdcParams;
    }

    public final LoginParams setLoginIdcParams(final LoginIdcParams loginIdcParams) {
        this.loginIdcParams = loginIdcParams;
        return this;
    }
}
