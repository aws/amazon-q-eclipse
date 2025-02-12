// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.auth;

import java.util.concurrent.CompletableFuture;

import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.GetSsoTokenResult;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.InvalidateSsoTokenParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.InvalidateSsoTokenResult;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;

public interface AuthTokenService {
    CompletableFuture<GetSsoTokenResult> getSsoToken(LoginType loginType, LoginParams loginParams, boolean loginOnInvalidToken);
    CompletableFuture<InvalidateSsoTokenResult> invalidateSsoToken(InvalidateSsoTokenParams invalidateSsoTokenParams);
}
