// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.auth;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.jsonrpc.messages.ResponseMessage;

import software.aws.toolkits.eclipse.amazonq.lsp.model.UpdateCredentialsPayload;

public interface AuthCredentialsService {
    CompletableFuture<ResponseMessage> updateTokenCredentials(UpdateCredentialsPayload params);
    CompletableFuture<Void> deleteTokenCredentials();
}
