// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import software.amazon.awssdk.utils.StringUtils;
import software.aws.toolkits.eclipse.amazonq.configuration.DefaultPluginStore;
import software.aws.toolkits.eclipse.amazonq.configuration.PluginStore;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.InvalidateSsoTokenParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginDetails;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginIdcParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;
import software.aws.toolkits.eclipse.amazonq.providers.LspProvider;
import software.aws.toolkits.eclipse.amazonq.providers.LspProviderImpl;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

public final class DefaultLoginService implements LoginService {
    private LspProvider lspProvider;
    private PluginStore pluginStore;
    private LoginType currentLogin;
    private LoginParams loginParams;

    private DefaultLoginService(final Builder builder) {
        this.lspProvider = Objects.requireNonNull(builder.lspProvider, "lspProvider cannot be null");
        this.pluginStore = Objects.requireNonNull(builder.pluginStore, "pluginStore cannot be null");
        String loginType = pluginStore.get(Constants.LOGIN_TYPE_KEY);
        currentLogin = StringUtils.isEmpty(loginType) || !isValidLoginType(loginType) ? LoginType.NONE : LoginType.valueOf(loginType);
        loginParams = currentLogin.equals(LoginType.IAM_IDENTITY_CENTER)
                ? new LoginParams().setLoginIdcParams(pluginStore.getObject(Constants.LOGIN_IDC_PARAMS_KEY, LoginIdcParams.class)) : null;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public CompletableFuture<Void> login(final LoginType loginType, final LoginParams loginParams) {
        updateConnectionDetails(loginType, loginParams);
        return CredentialUtils.getToken(lspProvider, currentLogin, loginParams, true)
            .thenCompose(token -> CredentialUtils.updateCredentials(lspProvider, token))
            .thenAccept((response) -> {
                updatePluginStore(loginType, loginParams);
            })
            .exceptionally(throwable -> {
                Activator.getLogger().error("Failed to sign in", throwable);
                throw new AmazonQPluginException(throwable);
            });
    }

    @Override
    public CompletableFuture<Void> logout() {
        if (currentLogin.equals(LoginType.NONE)) {
            Activator.getLogger().warn("Attempting to invalidate token in a logged out state");
            return CompletableFuture.completedFuture(null);
        }
        return CredentialUtils.getToken(lspProvider, currentLogin, loginParams, false)
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
                                          LoginDetails loginDetails = new LoginDetails();
                                          loginDetails.setIsLoggedIn(false);
                                          loginDetails.setLoginType(LoginType.NONE);
                                          AuthStatusProvider.notifyAuthStatusChanged(loginDetails);
                                          removeItemsFromPluginStore();
                                          updateConnectionDetails(LoginType.NONE, new LoginParams().setLoginIdcParams(null));
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
        if (currentLogin.equals(LoginType.NONE)) {
            return CompletableFuture.completedFuture(null);
        }
        return CredentialUtils.getToken(lspProvider, currentLogin, loginParams, false)
                .thenAccept(token -> CredentialUtils.updateCredentials(lspProvider, token))
                .exceptionally(throwable -> {
                    Activator.getLogger().error("Failed to update token", throwable);
                    throw new AmazonQPluginException(throwable);
                });
    }

    @Override
    public CompletableFuture<LoginDetails> getLoginDetails() {
        LoginDetails loginDetails = new LoginDetails();
        if (currentLogin.equals(LoginType.NONE)) {
            loginDetails.setIsLoggedIn(false);
            loginDetails.setLoginType(LoginType.NONE);
            loginDetails.setIssuerUrl(null);
            AuthStatusProvider.notifyAuthStatusChanged(loginDetails);
            return CompletableFuture.completedFuture(loginDetails);
        }
        return CredentialUtils.getToken(lspProvider, currentLogin, loginParams, false)
                .thenApply(ssoToken -> {
                    boolean isLoggedIn = ssoToken != null;
                    loginDetails.setIsLoggedIn(isLoggedIn);
                    loginDetails.setLoginType(isLoggedIn ? currentLogin : LoginType.NONE);
                    loginDetails.setIssuerUrl(getIssuerUrl(isLoggedIn));
                    AuthStatusProvider.notifyAuthStatusChanged(loginDetails);
                    return loginDetails;
                })
                .exceptionally(throwable -> {
                    Activator.getLogger().error("Failed to check login status", throwable);
                    loginDetails.setIsLoggedIn(false);
                    loginDetails.setLoginType(LoginType.NONE);
                    loginDetails.setIssuerUrl(null);
                    return loginDetails;
                });
    }

    public CompletableFuture<Boolean> reAuthenticate() {
        if (currentLogin.equals(LoginType.NONE)) {
            Activator.getLogger().warn("Reauthenticate called without an active login");
            return CompletableFuture.completedFuture(false);
        }

        return login(currentLogin, loginParams)
                .thenApply(loggedIn -> {
                    Activator.getLogger().info("Successfully reauthenticated");
                    return true;
                })
                .exceptionally(throwable -> {
                    Activator.getLogger().error("Failed to reauthenticate", throwable);
                    return false;
                });
    }

    private static boolean isValidLoginType(final String loginType) {
        try {
            LoginType.valueOf(loginType);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    private void updateConnectionDetails(final LoginType type, final LoginParams params) {
        currentLogin = type;
        loginParams = params;
    }

    private void updatePluginStore(final LoginType type, final LoginParams params) {
        pluginStore.put(Constants.LOGIN_TYPE_KEY, type.name());
        pluginStore.putObject(Constants.LOGIN_IDC_PARAMS_KEY, params.getLoginIdcParams());
    }

    private void removeItemsFromPluginStore() {
        pluginStore.remove(Constants.LOGIN_TYPE_KEY);
        pluginStore.remove(Constants.LOGIN_IDC_PARAMS_KEY);
    }

    private String getIssuerUrl(final boolean isLoggedIn) {
        if (!isLoggedIn || currentLogin.equals(LoginType.NONE)) {
            return null;
        }
        if (currentLogin.equals(LoginType.BUILDER_ID)) {
            return Constants.AWS_BUILDER_ID_URL;
        }
        return Objects.isNull(loginParams) || Objects.isNull(loginParams.getLoginIdcParams()) ? null : loginParams.getLoginIdcParams().getUrl();
    }

    public static class Builder {
        private LspProvider lspProvider;
        private PluginStore pluginStore;

        public final Builder withLspProvider(final LspProviderImpl lspProvider) {
            this.lspProvider = lspProvider;
            return this;
        }
        public final Builder withPluginStore(final PluginStore pluginStore) {
            this.pluginStore = pluginStore;
            return this;
        }

        public final DefaultLoginService build() {
            if (lspProvider == null) {
                lspProvider = Activator.getLspProvider();
            }
            if (pluginStore == null) {
                pluginStore = DefaultPluginStore.getInstance();
            }
            DefaultLoginService instance = new DefaultLoginService(this);
            instance.updateToken();
            return instance;
        }
    }
}
