// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.lsp4j.jsonrpc.messages.ResponseMessage;

import software.amazon.awssdk.services.toolkittelemetry.model.AWSProduct;
import software.aws.toolkits.eclipse.amazonq.configuration.PluginStore;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.AuthStateManager;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.GetSsoTokenOptions;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.GetSsoTokenParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.GetSsoTokenSource;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.InvalidateSsoTokenParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.Profile;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.ProfileSettings;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.SsoSession;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.SsoSessionSettings;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.SsoToken;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.UpdateProfileOptions;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.UpdateProfileParams;
import software.aws.toolkits.eclipse.amazonq.lsp.encryption.DefaultLspEncryptionManager;
import software.aws.toolkits.eclipse.amazonq.lsp.encryption.LspEncryptionManager;
import software.aws.toolkits.eclipse.amazonq.lsp.model.BearerCredentials;
import software.aws.toolkits.eclipse.amazonq.lsp.model.UpdateCredentialsPayload;
import software.aws.toolkits.eclipse.amazonq.lsp.model.UpdateCredentialsPayloadData;
import software.aws.toolkits.eclipse.amazonq.providers.LspProvider;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

import static software.aws.toolkits.eclipse.amazonq.util.QConstants.Q_SCOPES;

public final class DefaultLoginService implements LoginService {
    private LspProvider lspProvider;
    private LspEncryptionManager encryptionManager;
    private AuthStateManager authStateManager;

    private DefaultLoginService(final Builder builder) {
        this.lspProvider = Objects.requireNonNull(builder.lspProvider, "lspProvider cannot be null");
        this.encryptionManager = Objects.requireNonNull(builder.encryptionManager, "encryption manager cannot be null");
        this.authStateManager = Objects.requireNonNull(builder.authStateManager, "authStateManager cannot be null");

        if (builder.initializeOnStartUp) {
            AuthState authState = authStateManager.getAuthState();
            if (authState.isExpired()) {
                reAuthenticate();
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public CompletableFuture<Void> login(final LoginType loginType, final LoginParams loginParams) {
        final AtomicReference<String> ssoTokenId = new AtomicReference<>(); // Saved for logout

        Activator.getLogger().info("Attempting to log in using LoginType " + loginType);

        return getToken(loginType, loginParams, true)
            .thenCompose(ssoToken -> {
                ssoTokenId.set(ssoToken.id());
                return updateCredentials(ssoToken);
            })
            .thenRun(() -> {
                Activator.getLogger().info("Successfully logged in");
                authStateManager.toLoggedIn(loginType, loginParams, ssoTokenId.get());
            })
            .exceptionally(throwable -> {
                Activator.getLogger().error("Failed to log in");
                throw new AmazonQPluginException(throwable);
            });
    }

    @Override
    public CompletableFuture<Void> logout() {
        AuthState authState = getAuthState();

        if (authState.isLoggedOut()) {
            Activator.getLogger().warn("Attempted to log out while already in a logged out state");
            return CompletableFuture.completedFuture(null);
        }

        if (authState.ssoTokenId().isBlank()) {
            Activator.getLogger().warn("Attempted to log out with no ssoTokenId saved in auth state");
            return CompletableFuture.completedFuture(null);
        }

        InvalidateSsoTokenParams params = new InvalidateSsoTokenParams(authState.ssoTokenId());

        return lspProvider.getAmazonQServer()
                .thenAccept(server -> {
                    server.invalidateSsoToken(params);
                    server.deleteTokenCredentials();
                }).thenRun(() -> {
                    Activator.getLogger().info("Successfully logged out");
                    authStateManager.toLoggedOut();
                }).exceptionally(throwable -> {
                    Activator.getLogger().error("Failed to log out");
                    throw new AmazonQPluginException(throwable);
                });
    }

    @Override
    public CompletableFuture<Void> reAuthenticate() {
        AuthState authState = authStateManager.getAuthState();

        if (authState.isLoggedOut()) {
            Activator.getLogger().warn("Attempted to re-authenticate while user is in a logged out state");
            return CompletableFuture.completedFuture(null);
        }

        return login(authState.loginType(), authState.loginParams());
    }

    @Override
    public AuthState getAuthState() {
        return authStateManager.getAuthState();
    }

    private CompletableFuture<SsoToken> getToken(final LoginType loginType, final LoginParams loginParams, final boolean triggerlogIn) {
        GetSsoTokenParams getSsoTokenParams = createGetSsoTokenParams(loginType, triggerlogIn);
        return lspProvider.getAmazonQServer()
                .thenApply(server -> {
                    if (triggerlogIn && loginType.equals(LoginType.IAM_IDENTITY_CENTER)) {
                        var profile = new Profile();
                        profile.setName(Constants.IDC_PROFILE_NAME);
                        profile.setProfileKinds(Collections.singletonList(Constants.IDC_PROFILE_KIND));
                        profile.setProfileSettings(new ProfileSettings(loginParams.getLoginIdcParams().getRegion(), Constants.IDC_SESSION_NAME));
                        var ssoSession = new SsoSession();
                        ssoSession.setName(Constants.IDC_SESSION_NAME);
                        ssoSession.setSsoSessionSettings(new SsoSessionSettings(
                                loginParams.getLoginIdcParams().getUrl(),
                                loginParams.getLoginIdcParams().getRegion(),
                                Q_SCOPES)
                        );
                        var updateProfileOptions = new UpdateProfileOptions(true, true, true, false);
                        var updateProfileParams = new UpdateProfileParams(profile, ssoSession, updateProfileOptions);
                        try {
                            server.updateProfile(updateProfileParams).get();
                        } catch (Exception e) {
                            Activator.getLogger().error("Failed to update profile", e);
                        }
                    }
                    return server;
                })
                .thenCompose(server -> server.getSsoToken(getSsoTokenParams))
                .thenApply(response -> {
                    return response.ssoToken();
                })
                .exceptionally(throwable -> {
                    Activator.getLogger().error("Failed to fetch SSO token from LSP", throwable);
                    throw new AmazonQPluginException(throwable);
                });
    }

    private CompletableFuture<ResponseMessage> updateCredentials(final SsoToken ssoToken) {
        String decryptedToken = decryptSsoToken(ssoToken.accessToken());
        UpdateCredentialsPayload payload = createUpdateCredentialsPayload(decryptedToken);
        return lspProvider.getAmazonQServer()
                .thenCompose(server -> server.updateTokenCredentials(payload))
                .exceptionally(throwable -> {
                    Activator.getLogger().error("Failed to update credentials with AmazonQ server", throwable);
                    throw new AmazonQPluginException(throwable);
                });
    }

    private String decryptSsoToken(final String encryptedSsoToken) {
        String decryptedToken = encryptionManager.decrypt(encryptedSsoToken);
        return decryptedToken.substring(1, decryptedToken.length() - 1); // Remove extra quotes surrounding token
    }

    private UpdateCredentialsPayload createUpdateCredentialsPayload(final String ssoToken) {
        BearerCredentials credentials = new BearerCredentials();
        credentials.setToken(ssoToken);

        UpdateCredentialsPayloadData data = new UpdateCredentialsPayloadData(credentials);
        String encryptedData = encryptionManager.encrypt(data);
        return new UpdateCredentialsPayload(encryptedData, true);
    }

    private GetSsoTokenParams createGetSsoTokenParams(final LoginType currentLogin, final boolean triggerlogIn) {
        GetSsoTokenSource source = currentLogin.equals(LoginType.IAM_IDENTITY_CENTER)
                ? new GetSsoTokenSource(LoginType.IAM_IDENTITY_CENTER.getValue(), null, Constants.IDC_PROFILE_NAME)
                : new GetSsoTokenSource(LoginType.BUILDER_ID.getValue(), Q_SCOPES, null);
        GetSsoTokenOptions options = new GetSsoTokenOptions(triggerlogIn);
        return new GetSsoTokenParams(source, AWSProduct.AMAZON_Q_FOR_ECLIPSE.toString(), options);
    }

    public static class Builder {
        private LspProvider lspProvider;
        private PluginStore pluginStore;
        private LspEncryptionManager encryptionManager;
        private AuthStateManager authStateManager;
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
                authStateManager = new AuthStateManager(pluginStore);
            }
            DefaultLoginService instance = new DefaultLoginService(this);
            return instance;
        }
    }
}
