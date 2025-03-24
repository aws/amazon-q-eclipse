// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import java.util.Objects;

import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;

public final class AuthUtil {
    private AuthUtil() {
        // Prevent instantiation
    }

    public static String getIssuerUrl(final LoginType loginType, final LoginParams loginParams) {
        if (loginType == null || loginType.equals(LoginType.NONE)) {
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

    public static void validateLoginParameters(final LoginType loginType, final LoginParams loginParams) {
        if (loginType == null) {
            throw new IllegalArgumentException("Missing required parameter: loginType cannot be null");
        }

        if (loginType.equals(LoginType.NONE)) {
            throw new IllegalArgumentException("Invalid loginType: NONE is not a valid login type");
        }

        if (loginParams == null) {
            throw new IllegalArgumentException("Missing required parameter: loginParams cannot be null");
        }
    }
}
