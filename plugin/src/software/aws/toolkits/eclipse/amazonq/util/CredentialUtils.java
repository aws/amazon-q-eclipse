// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import software.amazon.awssdk.services.toolkittelemetry.model.AWSProduct;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.SsoSession;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.SsoToken;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.GetSsoTokenOptions;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.GetSsoTokenParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.GetSsoTokenSource;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginDetails;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.Profile;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.UpdateProfileParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.UpdateProfileOptions;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.SsoSessionSettings;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.ProfileSettings;
import software.aws.toolkits.eclipse.amazonq.lsp.encryption.LspEncryptionManager;
import software.aws.toolkits.eclipse.amazonq.lsp.model.BearerCredentials;
import software.aws.toolkits.eclipse.amazonq.lsp.model.UpdateCredentialsPayload;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.providers.LspProvider;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseMessage;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static software.aws.toolkits.eclipse.amazonq.util.QConstants.Q_SCOPES;

public final class CredentialUtils {
    private CredentialUtils() {
        // Private constructor to prevent instantiation
    }
    public static CompletableFuture<SsoToken> getToken(
    final LspProvider lspProvider,
    final LoginType currentLogin,
    final LoginParams loginParams,
    final boolean triggerSignIn) {

        GetSsoTokenParams params = getSsoTokenParams(currentLogin, triggerSignIn);
        String issuerUrl = (currentLogin.equals(LoginType.IAM_IDENTITY_CENTER))
                ? loginParams.getLoginIdcParams().getUrl()
                : Constants.AWS_BUILDER_ID_URL;

        return lspProvider.getAmazonQServer()
                .thenApply(server -> {
                    if (currentLogin.equals(LoginType.IAM_IDENTITY_CENTER) && triggerSignIn) {
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
                .thenCompose(server -> server.getSsoToken(params)
                        .thenApply(response -> {
                            if (response != null && response.ssoToken() != null && triggerSignIn) {
                                LoginDetails loginDetails = new LoginDetails();
                                loginDetails.setIsLoggedIn(true);
                                loginDetails.setLoginType(currentLogin);
                                loginDetails.setIssuerUrl(issuerUrl);
                                AuthStatusProvider.notifyAuthStatusChanged(loginDetails);
                            }
                            return response.ssoToken();
                        }))
                .exceptionally(throwable -> {
                    Activator.getLogger().error("Failed to fetch SSO token from LSP", throwable);
                    throw new AmazonQPluginException(throwable);
                });
    }
    public static CompletableFuture<ResponseMessage> updateCredentials(final LspProvider lspProvider, final SsoToken ssoToken) {
        BearerCredentials credentials = new BearerCredentials();
        var decryptedToken = LspEncryptionManager.getInstance().decrypt(ssoToken.accessToken());
        decryptedToken = decryptedToken.substring(1, decryptedToken.length() - 1);
        credentials.setToken(decryptedToken);
        UpdateCredentialsPayload updateCredentialsPayload = new UpdateCredentialsPayload();
        updateCredentialsPayload.setData(credentials);
        updateCredentialsPayload.setEncrypted(false);
        return lspProvider.getAmazonQServer()
                .thenCompose(server -> server.updateTokenCredentials(updateCredentialsPayload))
                .exceptionally(throwable -> {
                    Activator.getLogger().error("Failed to update credentials with AmazonQ server", throwable);
                    throw new AmazonQPluginException(throwable);
                });
    }
    private static GetSsoTokenParams getSsoTokenParams(final LoginType currentLogin, final boolean triggerSignIn) {
        GetSsoTokenSource source = currentLogin.equals(LoginType.IAM_IDENTITY_CENTER)
                ? new GetSsoTokenSource(LoginType.IAM_IDENTITY_CENTER.getValue(), null, Constants.IDC_PROFILE_NAME)
                : new GetSsoTokenSource(LoginType.BUILDER_ID.getValue(), Q_SCOPES, null);
        GetSsoTokenOptions options = new GetSsoTokenOptions(triggerSignIn);
        return new GetSsoTokenParams(source, AWSProduct.AMAZON_Q_FOR_ECLIPSE.toString(), options);
    }
}
