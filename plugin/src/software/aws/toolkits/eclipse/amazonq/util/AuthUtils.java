// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.GetSsoTokenOptions;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.GetSsoTokenParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.GetSsoTokenSource;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.InvalidateSsoTokenParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.SsoToken;
import software.aws.toolkits.eclipse.amazonq.lsp.model.BearerCredentials;
import software.aws.toolkits.eclipse.amazonq.lsp.model.UpdateCredentialsPayload;
import software.aws.toolkits.eclipse.amazonq.providers.LspProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.jsonrpc.messages.ResponseMessage;

import static software.aws.toolkits.eclipse.amazonq.util.QConstants.Q_PRODUCT_NAME;
import static software.aws.toolkits.eclipse.amazonq.util.QConstants.Q_SCOPES;

public final class AuthUtils {

    private AuthUtils() {
        // Prevent instantiation
    }

    private static final List<AuthStatusChangedListener> LISTENERS = new ArrayList<>();

    public static void addAuthStatusChangeListener(final AuthStatusChangedListener listener) {
        LISTENERS.add(listener);
    }

    public static void removeAuthStatusChangeListener(final AuthStatusChangedListener listener) {
        LISTENERS.remove(listener);
    }

    public static CompletableFuture<ResponseMessage> signIn() {
        return getSsoToken(true)
                .thenCompose(AuthUtils::updateCredentials)
                .exceptionally(throwable -> {
                    PluginLogger.error("Failed to sign in", throwable);
                    throw new AmazonQPluginException(throwable);
                });
    }

    public static CompletableFuture<ResponseMessage> updateToken() {
        return getSsoToken(false)
                .thenCompose(AuthUtils::updateCredentials)
                .exceptionally(throwable -> {
                    PluginLogger.error("Failed to update token", throwable);
                    throw new AmazonQPluginException(throwable);
                });
    }

    public static CompletableFuture<Boolean> isLoggedIn() {
        return getSsoToken(false)
                .thenApply(ssoToken -> {
                    boolean isLoggedIn = ssoToken != null;
                    notifyAuthStatusChanged(isLoggedIn);
                    return isLoggedIn;
                })
                .exceptionally(throwable -> {
                    PluginLogger.error("Failed to check login status", throwable);
                    return false;
                });
    }

    public static CompletableFuture<Void> invalidateToken() {
        return getSsoToken(false)
                .thenCompose(currentToken -> {
                    if (currentToken == null) {
                        PluginLogger.warn("Attempting to invalidate token with no active auth session");
                        return CompletableFuture.completedFuture(null);
                    }
                    String ssoTokenId = currentToken.id();
                    InvalidateSsoTokenParams params = new InvalidateSsoTokenParams(ssoTokenId);
                    return LspProvider.getAuthServer()
                                      .thenCompose(server -> server.invalidateSsoToken(params))
                                      .thenRun(() -> notifyAuthStatusChanged(false))
                                      .exceptionally(throwable -> {
                                          PluginLogger.error("Unexpected error while invalidating token", throwable);
                                          throw new AmazonQPluginException(throwable);
                                      });
                });
    }

    private static CompletableFuture<SsoToken> getSsoToken(final boolean triggerSignIn) {
        GetSsoTokenSource source = new GetSsoTokenSource(Q_PRODUCT_NAME, "AwsBuilderId", null, null);
        GetSsoTokenOptions options = new GetSsoTokenOptions(true, true, triggerSignIn);
        GetSsoTokenParams params = new GetSsoTokenParams(source, Q_SCOPES, options);
        return LspProvider.getAuthServer()
                           .thenCompose(server -> server.getSsoToken(params)
                                                        .thenApply(response -> {
                                                            if (triggerSignIn) {
                                                                notifyAuthStatusChanged(true);
                                                            }
                                                            return response.ssoToken();
                                                        }))
                           .exceptionally(throwable -> {
                               PluginLogger.error("Failed to fetch SSO token from LSP", throwable);
                               throw new AmazonQPluginException(throwable);
                           });
    }

    private static CompletableFuture<ResponseMessage> updateCredentials(final SsoToken ssoToken) {
        BearerCredentials credentials = new BearerCredentials();
        credentials.setToken(ssoToken.accessToken());
        UpdateCredentialsPayload updateCredentialsPayload = new UpdateCredentialsPayload();
        updateCredentialsPayload.setData(credentials);
        updateCredentialsPayload.setEncrypted(false);
        return LspProvider.getAmazonQServer()
                           .thenCompose(server -> server.updateTokenCredentials(updateCredentialsPayload))
                           .exceptionally(throwable -> {
                               PluginLogger.error("Failed to update credentials with AmazonQ server", throwable);
                               throw new AmazonQPluginException(throwable);
                           });
    }

    private static void notifyAuthStatusChanged(final boolean isLoggedIn) {
        for (AuthStatusChangedListener listener : LISTENERS) {
            listener.onAuthStatusChanged(isLoggedIn);
        }
    }
}
