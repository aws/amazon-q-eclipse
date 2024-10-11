// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import static software.aws.toolkits.eclipse.amazonq.util.QConstants.Q_PRODUCT_NAME;
import static software.aws.toolkits.eclipse.amazonq.util.QConstants.Q_SCOPES;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseMessage;
import software.amazon.awssdk.utils.StringUtils;
import software.aws.toolkits.eclipse.amazonq.configuration.PluginStore;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.GetSsoTokenOptions;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.GetSsoTokenParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.GetSsoTokenSource;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.InvalidateSsoTokenParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginDetails;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginIdcParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.SsoToken;
import software.aws.toolkits.eclipse.amazonq.lsp.model.BearerCredentials;
import software.aws.toolkits.eclipse.amazonq.lsp.model.UpdateCredentialsPayload;
import software.aws.toolkits.eclipse.amazonq.providers.LspProvider;

public final class DefaultLoginService implements LoginService {
    private static DefaultLoginService instance;
    private static LoginType currentLogin;
    private static LoginParams loginParams;
    private static final List<AuthStatusChangedListener> LISTENERS = new ArrayList<>();

    private DefaultLoginService() {
        // prevent initialization
    }

    private static boolean isValidLoginType(final String loginType) {
        try {
            LoginType.valueOf(loginType);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    public static synchronized DefaultLoginService getInstance() {
        if (instance == null) {
            instance = new DefaultLoginService();
            String loginType = PluginStore.get(Constants.LOGIN_TYPE_KEY);
            currentLogin = StringUtils.isEmpty(loginType) || !isValidLoginType(loginType) ? LoginType.NONE : LoginType.valueOf(loginType);
            loginParams = currentLogin.equals(LoginType.IAM_IDENTITY_CENTER)
                    ? new LoginParams().setLoginIdcParams(PluginStore.getObject(Constants.LOGIN_IDC_PARAMS_KEY, LoginIdcParams.class)) : null;
            instance.updateToken();
        }
        return instance;
    }

    private void updateConnectionDetails(final LoginType type, final LoginParams params) {
        currentLogin = type;
        loginParams = params;
    }

    private static void updatePluginStore(final LoginType type, final LoginParams params) {
        PluginStore.put(Constants.LOGIN_TYPE_KEY, type.name());
        PluginStore.putObject(Constants.LOGIN_IDC_PARAMS_KEY, params.getLoginIdcParams());
    }

    private static void removeItemsFromPluginStore() {
        PluginStore.remove(Constants.LOGIN_TYPE_KEY);
        PluginStore.remove(Constants.LOGIN_IDC_PARAMS_KEY);
    }

    private CompletableFuture<SsoToken> getToken(final boolean triggerSignIn) {
        GetSsoTokenSource source;
        if (currentLogin.equals(LoginType.IAM_IDENTITY_CENTER)) {
            source = new GetSsoTokenSource(Q_PRODUCT_NAME, "IamIdentityCenter",
                    loginParams.getLoginIdcParams().getUrl(), loginParams.getLoginIdcParams().getRegion());
        } else {
            source = new GetSsoTokenSource(Q_PRODUCT_NAME, "AwsBuilderId", null, null);
        }
        GetSsoTokenOptions options = new GetSsoTokenOptions(true, true, triggerSignIn);
        GetSsoTokenParams params = new GetSsoTokenParams(source, Q_SCOPES, options);
        return LspProvider.getAuthServer()
                           .thenCompose(server -> server.getSsoToken(params)
                                                        .thenApply(response -> {
                                                            if (triggerSignIn) {
                                                                LoginDetails loginDetails = new LoginDetails();
                                                                loginDetails.setIsLoggedIn(true);
                                                                loginDetails.setLoginType(currentLogin);
                                                                notifyAuthStatusChanged(loginDetails);
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

    public static void addAuthStatusChangeListener(final AuthStatusChangedListener listener) {
        LISTENERS.add(listener);
    }

    public static void removeAuthStatusChangeListener(final AuthStatusChangedListener listener) {
        LISTENERS.remove(listener);
    }

    private static void notifyAuthStatusChanged(final LoginDetails loginDetails) {
        for (AuthStatusChangedListener listener : LISTENERS) {
            listener.onAuthStatusChanged(loginDetails);
        }
    }

    @Override
    public CompletableFuture<Void> login(final LoginType loginType, final LoginParams loginParams) {
        updateConnectionDetails(loginType, loginParams);
        return getToken(true)
        .thenCompose(DefaultLoginService::updateCredentials)
        .thenAccept((response) -> {
            DefaultLoginService.updatePluginStore(loginType, loginParams);
        })
        .exceptionally(throwable -> {
            PluginLogger.error("Failed to sign in", throwable);
            throw new AmazonQPluginException(throwable);
        });
    }

    @Override
    public CompletableFuture<Void> logout() {
        if (currentLogin.equals(LoginType.NONE)) {
            return CompletableFuture.completedFuture(null);
        }
        return getToken(false)
                .thenCompose(currentToken -> {
                    if (currentToken == null) {
                        PluginLogger.warn("Attempting to invalidate token with no active auth session");
                        return CompletableFuture.completedFuture(null);
                    }
                    String ssoTokenId = currentToken.id();
                    InvalidateSsoTokenParams params = new InvalidateSsoTokenParams(ssoTokenId);
                    return LspProvider.getAuthServer()
                                      .thenCompose(server -> server.invalidateSsoToken(params))
                                      .thenRun(() -> {
                                          LoginDetails loginDetails = new LoginDetails();
                                          loginDetails.setIsLoggedIn(false);
                                          loginDetails.setLoginType(LoginType.NONE);
                                          notifyAuthStatusChanged(loginDetails);
                                          removeItemsFromPluginStore();
                                          updateConnectionDetails(LoginType.NONE, new LoginParams().setLoginIdcParams(null));
                                      })
                                      .exceptionally(throwable -> {
                                          PluginLogger.error("Unexpected error while invalidating token", throwable);
                                          throw new AmazonQPluginException(throwable);
                                      });
                });
    }

    @Override
    public CompletableFuture<Void> updateToken() {
        if (currentLogin.equals(LoginType.NONE)) {
            return CompletableFuture.completedFuture(null);
        }
        return getToken(false)
                .thenCompose(DefaultLoginService::updateCredentials)
                .thenRun(() -> CompletableFuture.completedFuture(null))
                .exceptionally(throwable -> {
                    PluginLogger.error("Failed to update token", throwable);
                    throw new AmazonQPluginException(throwable);
                });
    }

    @Override
    public CompletableFuture<LoginDetails> getLoginDetails() {
        LoginDetails loginDetails = new LoginDetails();
        if (currentLogin.equals(LoginType.NONE)) {
            return CompletableFuture.supplyAsync(() -> {
                loginDetails.setIsLoggedIn(false);
                loginDetails.setLoginType(LoginType.NONE);
                notifyAuthStatusChanged(loginDetails);
                return loginDetails;
            });
        }
        return getToken(false)
                .thenApply(ssoToken -> {
                    boolean isLoggedIn = ssoToken != null;
                    loginDetails.setIsLoggedIn(isLoggedIn);
                    loginDetails.setLoginType(isLoggedIn ? currentLogin : LoginType.NONE);
                    notifyAuthStatusChanged(loginDetails);
                    return loginDetails;
                })
                .exceptionally(throwable -> {
                    PluginLogger.error("Failed to check login status", throwable);
                    loginDetails.setIsLoggedIn(false);
                    loginDetails.setLoginType(LoginType.NONE);
                    return loginDetails;
                });
    }
}
