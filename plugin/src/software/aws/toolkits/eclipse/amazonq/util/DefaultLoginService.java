// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.jsonrpc.messages.ResponseMessage;

import software.amazon.awssdk.services.toolkittelemetry.model.AWSProduct;
import software.aws.toolkits.eclipse.amazonq.configuration.PluginStore;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.GetSsoTokenOptions;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.GetSsoTokenParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.GetSsoTokenSource;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.InvalidateSsoTokenParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginDetails;
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
import software.aws.toolkits.eclipse.amazonq.views.model.AuthState;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

import static software.aws.toolkits.eclipse.amazonq.util.QConstants.Q_SCOPES;

public final class DefaultLoginService implements LoginService {
    private LspProvider lspProvider;
    private PluginStore pluginStore;
    private LspEncryptionManager encryptionManager;
    private AuthStateManager authStateManager;

    private DefaultLoginService(final Builder builder) {
        this.lspProvider = Objects.requireNonNull(builder.lspProvider, "lspProvider cannot be null");
        this.pluginStore = Objects.requireNonNull(builder.pluginStore, "pluginStore cannot be null");
        this.encryptionManager = Objects.requireNonNull(builder.encryptionManager, "encryption manager cannot be null");
        this.authStateManager = new AuthStateManager(pluginStore);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public CompletableFuture<Void> login(final LoginType loginType, final LoginParams loginParams) {
        return getToken(loginType, loginParams, true)
            .thenApply(this::updateCredentials)
            .thenAccept(responseMessage -> {
                authStateManager.toLoggedIn(loginType, loginParams);
            })
            .exceptionally(throwable -> {
                Activator.getLogger().error("Failed to sign in", throwable);
                throw new AmazonQPluginException(throwable);
            });
    }

    @Override
    public CompletableFuture<Void> logout() {
        if (authStateManager.getAuthState().isLoggedOut()) {
            Activator.getLogger().warn("Attempting to invalidate token in a logged out state");
            return CompletableFuture.completedFuture(null);
        }

        AuthState authState = authStateManager.getAuthState();
        LoginType loginType = authState.loginType();
        LoginParams loginParams = authState.loginParams();

        return getToken(loginType, loginParams, false)
                .thenCompose(currentToken -> {
                    if (currentToken == null) {
                        Activator.getLogger().warn("Attempting to invalidate token with no active auth session");
                        return CompletableFuture.completedFuture(null);
                    }
                    String ssoTokenId = currentToken.id();
                    InvalidateSsoTokenParams params = new InvalidateSsoTokenParams(ssoTokenId);
                    return lspProvider.getAmazonQServer()
                                      .thenCompose(server -> server.invalidateSsoToken(params))
                                      .thenRun(() -> {
                                          authStateManager.toLoggedOut();
                                      })
                                      .exceptionally(throwable -> {
                                          Activator.getLogger().error("Unexpected error while invalidating token", throwable);
                                          throw new AmazonQPluginException(throwable);
                                      });
                });
    }

    @Override
    public CompletableFuture<Void> updateToken() {
        // TODO: do not expose this method to callers. token updates should be handled by the login service
        // upon initialization, login/logout, token update, or reauth.
        if (authStateManager.getAuthState().isLoggedOut()) {
            return CompletableFuture.completedFuture(null);
        }

        AuthState authState = authStateManager.getAuthState();
        LoginType loginType = authState.loginType();
        LoginParams loginParams = authState.loginParams();

        return getToken(loginType, loginParams, false)
                .thenAccept(this::updateCredentials)
                .exceptionally(throwable -> {
                    Activator.getLogger().error("Failed to update token", throwable);
                    throw new AmazonQPluginException(throwable);
                });
    }

    @Override
    public CompletableFuture<LoginDetails> getLoginDetails() {
        AuthState authState = authStateManager.getAuthState();
        LoginType loginType = authState.loginType();
        LoginParams loginParams = authState.loginParams();

        if (authState.isLoggedOut()) {
            return CompletableFuture.completedFuture(authState.toLoginDetails());
        }

        return getToken(loginType, loginParams, false)
                .thenApply(ssoToken -> {
                    boolean isLoggedIn = ssoToken != null;
                    if (isLoggedIn) {
                        authStateManager.toLoggedIn(loginType, loginParams);
                    } else {
                        authStateManager.toLoggedOut();
                    }

                    return authState.toLoginDetails();
                })
                .exceptionally(throwable -> {
                    // TODO update to attempt a sign in if token retrieval fails https://sim.amazon.com/issues/ECLIPSE-457
                    Activator.getLogger().error("Failed to check login status", throwable);
                    authStateManager.toLoggedOut();
                    return authState.toLoginDetails();
                });
    }

    public CompletableFuture<Boolean> reAuthenticate() {
        AuthState authState = authStateManager.getAuthState();

        if (authState.isLoggedOut()) {
            Activator.getLogger().info("Unable to proceed with re-authenticateing. User is in a logged out state.");
            return CompletableFuture.completedFuture(false);
        }

        LoginType loginType = authState.loginType();
        LoginParams loginParams = authState.loginParams();

        Activator.getLogger().info("Attempting to re-authenticate using login type " + loginType.name());

        return login(loginType, loginParams)
                .thenApply(loggedIn -> {
                    Activator.getLogger().info("Successfully reauthenticated");
                    return true;
                })
                .exceptionally(throwable -> {
                    Activator.getLogger().error("Failed to reauthenticate", throwable);
                    return false;
                });
    }

    CompletableFuture<SsoToken> getToken(final LoginType loginType, final LoginParams loginParams, final boolean triggerSignIn) {

        GetSsoTokenParams getSsoTokenParams = getSsoTokenParams(loginType, triggerSignIn);

        return lspProvider.getAmazonQServer()
                .thenApply(server -> {
                    if (triggerSignIn && loginType.equals(LoginType.IAM_IDENTITY_CENTER)) {
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
                .thenCompose(server -> server.getSsoToken(getSsoTokenParams)
                        .thenApply(response -> {
                            if (triggerSignIn) {
                                authStateManager.toLoggedIn(loginType, loginParams);
                            }
                            return response.ssoToken();
                        }))
                .exceptionally(throwable -> {
                    Activator.getLogger().error("Failed to fetch SSO token from LSP", throwable);
                    authStateManager.toLoggedOut();
                    throw new AmazonQPluginException(throwable);
                });
    }

    CompletableFuture<ResponseMessage> updateCredentials(final SsoToken ssoToken) {
        BearerCredentials credentials = new BearerCredentials();
        var decryptedToken = encryptionManager.decrypt(ssoToken.accessToken());
        decryptedToken = decryptedToken.substring(1, decryptedToken.length() - 1);
        credentials.setToken(decryptedToken);
        UpdateCredentialsPayloadData data = new UpdateCredentialsPayloadData(credentials);
        String encryptedData = encryptionManager.encrypt(data);
        UpdateCredentialsPayload updateCredentialsPayload = new UpdateCredentialsPayload(encryptedData, true);
        return lspProvider.getAmazonQServer()
                .thenCompose(server -> server.updateTokenCredentials(updateCredentialsPayload))
                .exceptionally(throwable -> {
                    Activator.getLogger().error("Failed to update credentials with AmazonQ server", throwable);
                    throw new AmazonQPluginException(throwable);
                });
    }

    private GetSsoTokenParams getSsoTokenParams(final LoginType currentLogin, final boolean triggerSignIn) {
        GetSsoTokenSource source = currentLogin.equals(LoginType.IAM_IDENTITY_CENTER)
                ? new GetSsoTokenSource(LoginType.IAM_IDENTITY_CENTER.getValue(), null, Constants.IDC_PROFILE_NAME)
                : new GetSsoTokenSource(LoginType.BUILDER_ID.getValue(), Q_SCOPES, null);
        GetSsoTokenOptions options = new GetSsoTokenOptions(triggerSignIn);
        return new GetSsoTokenParams(source, AWSProduct.AMAZON_Q_FOR_ECLIPSE.toString(), options);
    }

    public static class Builder {
        private LspProvider lspProvider;
        private PluginStore pluginStore;
        private LspEncryptionManager encryptionManager;
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
            DefaultLoginService instance = new DefaultLoginService(this);
            if (initializeOnStartUp) {
                instance.updateToken();
            }
            return instance;
        }
    }
}
