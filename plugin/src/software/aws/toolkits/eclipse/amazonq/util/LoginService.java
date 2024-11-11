// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import java.util.concurrent.CompletableFuture;

import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginDetails;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;

public interface LoginService {
    CompletableFuture<Void> login(LoginType loginType, LoginParams loginParams);
    CompletableFuture<Void> logout();
    CompletableFuture<Void> updateToken();
    CompletableFuture<LoginDetails> getLoginDetails();
    CompletableFuture<Boolean> reAuthenticate();
}
