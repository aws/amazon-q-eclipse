// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import static software.aws.toolkits.eclipse.amazonq.util.QConstants.Q_SCOPES;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.jsonrpc.messages.ResponseMessage;

import software.amazon.awssdk.services.toolkittelemetry.model.AWSProduct;
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
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.Profile;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.ProfileSettings;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.SsoToken;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.SsoSession;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.SsoSessionSettings;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.UpdateProfileOptions;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.UpdateProfileParams;
import software.aws.toolkits.eclipse.amazonq.lsp.encryption.LspEncryptionManager;
import software.aws.toolkits.eclipse.amazonq.lsp.model.BearerCredentials;
import software.aws.toolkits.eclipse.amazonq.lsp.model.UpdateCredentialsPayload;
import software.aws.toolkits.eclipse.amazonq.providers.LspProvider;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

public final class DefaultLoginService implements LoginService {
    private static DefaultLoginService instance;
    private static LoginType currentLogin;
    private static LoginParams loginParams;
    private static final List<AuthStatusChangedListener> LISTENERS = new ArrayList<>();

    private DefaultLoginService() {
        // prevent initialization
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

    @Override
    public CompletableFuture<Void> login(final LoginType loginType, final LoginParams loginParams) {
        updateConnectionDetails(loginType, loginParams);
        return getToken(true)
            .thenCompose(DefaultLoginService::updateCredentials)
            .thenAccept((response) -> {
                DefaultLoginService.updatePluginStore(loginType, loginParams);
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
        return getToken(false)
                .thenCompose(currentToken -> {
                    if (currentToken == null) {
                        Activator.getLogger().warn("Attempting to invalidate token with no active auth session");
                        return CompletableFuture.completedFuture(null);
                    }
                    String ssoTokenId = currentToken.id();
                    InvalidateSsoTokenParams params = new InvalidateSsoTokenParams(ssoTokenId);
                    return LspProvider.getAmazonQServer()
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
                                          Activator.getLogger().error("Unexpected error while invalidating token", throwable);
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
                    Activator.getLogger().error("Failed to update token", throwable);
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
                loginDetails.setIssuerUrl(null);
                notifyAuthStatusChanged(loginDetails);
                return loginDetails;
            });
        }
        return getToken(false)
                .thenApply(ssoToken -> {
                    boolean isLoggedIn = ssoToken != null;
                    loginDetails.setIsLoggedIn(isLoggedIn);
                    loginDetails.setLoginType(isLoggedIn ? currentLogin : LoginType.NONE);
                    loginDetails.setIssuerUrl(getIssuerUrl(isLoggedIn));
                    notifyAuthStatusChanged(loginDetails);
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

    private static void updatePluginStore(final LoginType type, final LoginParams params) {
        PluginStore.put(Constants.LOGIN_TYPE_KEY, type.name());
        PluginStore.putObject(Constants.LOGIN_IDC_PARAMS_KEY, params.getLoginIdcParams());
    }

    private static void removeItemsFromPluginStore() {
        PluginStore.remove(Constants.LOGIN_TYPE_KEY);
        PluginStore.remove(Constants.LOGIN_IDC_PARAMS_KEY);
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

    private CompletableFuture<SsoToken> getToken(final boolean triggerSignIn) {
        GetSsoTokenSource source = currentLogin.equals(LoginType.IAM_IDENTITY_CENTER)
                ? new GetSsoTokenSource(LoginType.IAM_IDENTITY_CENTER.getValue(), null, Constants.IDC_PROFILE_NAME)
                : new GetSsoTokenSource(LoginType.BUILDER_ID.getValue(), Q_SCOPES, null);

        GetSsoTokenOptions options = new GetSsoTokenOptions(triggerSignIn);
        GetSsoTokenParams params = new GetSsoTokenParams(source, AWSProduct.AMAZON_Q_FOR_ECLIPSE.toString(), options);

        return LspProvider.getAmazonQServer()
                .thenApply(server -> {
                    if (currentLogin.equals(LoginType.IAM_IDENTITY_CENTER) && triggerSignIn) {
                        var profile = new Profile();
                        profile.setName(Constants.IDC_PROFILE_NAME);
                        profile.setProfileKinds(Collections.singletonList(Constants.IDC_SESSION_NAME));
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
                    Activator.getLogger().error("Failed to fetch SSO token from LSP", throwable);
                    throw new AmazonQPluginException(throwable);
                });
    }

    private static CompletableFuture<ResponseMessage> updateCredentials(final SsoToken ssoToken) {
        BearerCredentials credentials = new BearerCredentials();
        var decryptedToken = LspEncryptionManager.getInstance().decrypt(ssoToken.accessToken());
        decryptedToken = decryptedToken.substring(1, decryptedToken.length() - 1);
        credentials.setToken(decryptedToken);
        UpdateCredentialsPayload updateCredentialsPayload = new UpdateCredentialsPayload();
        updateCredentialsPayload.setData(credentials);
        updateCredentialsPayload.setEncrypted(false);
        return LspProvider.getAmazonQServer()
                           .thenCompose(server -> server.updateTokenCredentials(updateCredentialsPayload))
                           .exceptionally(throwable -> {
                               Activator.getLogger().error("Failed to update credentials with AmazonQ server", throwable);
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

}
