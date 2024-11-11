// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.auth;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;


import software.aws.toolkits.eclipse.amazonq.configuration.PluginStore;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.InvalidateSsoTokenParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;
import software.aws.toolkits.eclipse.amazonq.lsp.encryption.DefaultLspEncryptionManager;
import software.aws.toolkits.eclipse.amazonq.lsp.encryption.LspEncryptionManager;
import software.aws.toolkits.eclipse.amazonq.providers.LspProvider;
import software.aws.toolkits.eclipse.amazonq.util.AuthUtil;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

public final class DefaultLoginService implements LoginService {
    private AuthStateManager authStateManager;
    private AuthTokenService authTokenService;
    private AuthCredentialsService authCredentialsService;

    private DefaultLoginService(final Builder builder) {
        this.authStateManager = Objects.requireNonNull(builder.authStateManager, "authStateManager cannot be null");
        this.authTokenService = Objects.requireNonNull(builder.authTokenService, "authTokenService cannot be null");
        this.authCredentialsService = Objects.requireNonNull(builder.authCredentialsService, "authCredentialsService cannot be null");

        if (builder.initializeOnStartUp) {
            AuthState authState = authStateManager.getAuthState();
            if (authState.isExpired()) {
                silentlyReAuthenticate();
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public CompletableFuture<Void> login(final LoginType loginType, final LoginParams loginParams) {
        if (authStateManager.getAuthState().isLoggedIn()) {
            Activator.getLogger().warn("Attempted to log in while already in a logged in state");
            return CompletableFuture.completedFuture(null);
        }

        Activator.getLogger().info("Attempting to login...");

        return processLogin(loginType, loginParams, true)
                .exceptionally(throwable -> {
                    Activator.getLogger().error("Failed to log in", throwable);
                    return null;
                });
    }

    @Override
    public CompletableFuture<Void> logout() {
        AuthState authState = getAuthState();

        if (authState.isLoggedOut()) {
            Activator.getLogger().warn("Attempted to log out while already in a logged out state");
            return CompletableFuture.completedFuture(null);
        }

        if (authState.ssoTokenId() == null || authState.ssoTokenId().isBlank()) {
            authStateManager.toLoggedOut();
            Activator.getLogger().warn("Attempted to log out with no ssoTokenId saved in auth state");
            return CompletableFuture.completedFuture(null);
        }

        Activator.getLogger().info("Attempting to log out...");

        InvalidateSsoTokenParams params = new InvalidateSsoTokenParams(authState.ssoTokenId());

        return authTokenService.invalidateSsoToken(params)
                .thenRun(() -> {
                    authCredentialsService.deleteTokenCredentials();
                })
                .thenRun(() -> {
                    authStateManager.toLoggedOut();
                    Activator.getLogger().info("Successfully logged out");
                })
                .exceptionally(throwable -> {
                    Activator.getLogger().error("Failed to log out", throwable);
                    return null;
                });
    }

    @Override
    public CompletableFuture<Void> expire() {
        Activator.getLogger().info("Attempting to expire credentials...");

        return authCredentialsService.updateTokenCredentials(null, false)
                .thenRun(() -> {
                    authStateManager.toExpired();
                    Activator.getLogger().info("Successfully expired credentials");
                })
                .exceptionally(throwable -> {
                    Activator.getLogger().error("Failed to expire credentials", throwable);
                    return null;
                });
    }

    @Override
    public CompletableFuture<Void> reAuthenticate() {
        AuthState authState = authStateManager.getAuthState();

        if (authState.isLoggedOut()) {
            Activator.getLogger().warn("Attempted to re-authenticate while user is in a logged out state");
            return CompletableFuture.completedFuture(null);
        }

        Activator.getLogger().info("Attempting to re-authenticate...");

        return processLogin(authState.loginType(), authState.loginParams(), true)
                .exceptionally(throwable -> {
                    Activator.getLogger().error("Failed to re-authenticate", throwable);
                    return null;
                });
    }

    @Override
    public CompletableFuture<Void> silentlyReAuthenticate() {
        AuthState authState = authStateManager.getAuthState();

        if (authState.isLoggedOut()) {
            Activator.getLogger().warn("Attempted to silently re-authenticate while user is in a logged out state");
            return CompletableFuture.completedFuture(null);
        }

        Activator.getLogger().info("Attempting to silently re-authenticate...");

        return processLogin(authState.loginType(), authState.loginParams(), false)
                .exceptionally(throwable -> {
                    Activator.getLogger().error("Failed to silently re-authenticate", throwable);
                    return null;
                });
    }


    @Override
    public AuthState getAuthState() {
        return authStateManager.getAuthState();
    }

    CompletableFuture<Void> processLogin(final LoginType loginType, final LoginParams loginParams, final boolean loginOnInvalidToken) {
        AuthUtil.validateLoginParameters(loginType, loginParams);

        final AtomicReference<String> ssoTokenId = new AtomicReference<>(); // Saved for logout

        return authTokenService.getSsoToken(loginType, loginParams, loginOnInvalidToken)
                .thenApply(ssoToken -> {
                    ssoTokenId.set(ssoToken.id());
                    return ssoToken;
                })
                .thenAccept(ssoToken -> {
                    authCredentialsService.updateTokenCredentials(ssoToken.accessToken(), true);
                })
                .thenRun(() -> {
                  authStateManager.toLoggedIn(loginType, loginParams, ssoTokenId.get());
                  Activator.getLogger().info("Successfully logged in");
              })
              .exceptionally(throwable -> {
                  throw new AmazonQPluginException("Failed to process log in", throwable);
              });
    }

    public static class Builder {
        private LspProvider lspProvider;
        private PluginStore pluginStore;
        private LspEncryptionManager encryptionManager;
        private AuthStateManager authStateManager;
        private AuthCredentialsService authCredentialsService;
        private AuthTokenService authTokenService;
        private boolean initializeOnStartUp;

        public final Builder withLspProvider(final LspProvider lspProvider) {
            this.lspProvider = lspProvider;
            return this;
        }
        public final Builder withPluginStore(final PluginStore pluginStore) {
            this.pluginStore = pluginStore;
            return this;
        }
        public final Builder withEncryptionManager(final LspEncryptionManager encryptionManager) {
            this.encryptionManager = encryptionManager;
            return this;
        }
        public final Builder withAuthStateManager(final AuthStateManager authStateManager) {
            this.authStateManager = authStateManager;
            return this;
        }
        public final Builder withAuthCredentialsService(final AuthCredentialsService qCredentialService) {
            this.authCredentialsService = qCredentialService;
            return this;
        }
        public final Builder withAuthTokenService(final AuthTokenService authTokenService) {
            this.authTokenService = authTokenService;
            return this;
        }
        public final Builder initializeOnStartUp() {
            this.initializeOnStartUp = true;
            return this;
        }
        public final DefaultLoginService build() {
            if (lspProvider == null) {
                lspProvider = Activator.getLspProvider();
            }
            if (pluginStore == null) {
                pluginStore = Activator.getPluginStore();
            }
            if (encryptionManager == null) {
                encryptionManager = DefaultLspEncryptionManager.getInstance();
            }
            if (authStateManager == null) {
                authStateManager = new DefaultAuthStateManager(pluginStore);
            }
            if (authCredentialsService == null) {
                authCredentialsService = DefaultAuthCredentialsService.builder()
                        .withLspProvider(lspProvider)
                        .withEncryptionManager(encryptionManager)
                        .build();
            }
            if (authTokenService == null) {
                authTokenService = DefaultAuthTokenService.builder()
                        .withLspProvider(lspProvider)
                        .build();
            }
            DefaultLoginService instance = new DefaultLoginService(this);
            return instance;
        }
    }
}
