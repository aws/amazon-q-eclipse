// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import static software.aws.toolkits.eclipse.amazonq.util.QConstants.Q_PRODUCT_NAME;
import static software.aws.toolkits.eclipse.amazonq.util.QConstants.Q_SCOPES;

import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseMessage;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.SsoToken;
import software.aws.toolkits.eclipse.amazonq.lsp.model.BearerCredentials;
import software.aws.toolkits.eclipse.amazonq.lsp.model.UpdateCredentialsPayload;
import software.aws.toolkits.eclipse.amazonq.providers.LspProvider;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.GetSsoTokenOptions;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.GetSsoTokenParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.GetSsoTokenSource;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginDetails;

public final class CredentialUtils {

    private CredentialUtils() {
        // Private constructor to prevent instantiation
    }
    public static CompletableFuture<ResponseMessage> updateCredentials(final LspProvider lspProvider, final SsoToken ssoToken) {
        BearerCredentials credentials = new BearerCredentials();
        credentials.setToken(ssoToken.accessToken());
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
    public static CompletableFuture<SsoToken> getToken(
    final LspProvider lspProvider,
    final LoginType currentLogin,
    final LoginParams loginParams,
    final boolean triggerSignIn) {
        GetSsoTokenSource source;
        if (currentLogin.equals(LoginType.IAM_IDENTITY_CENTER)) {
            source = new GetSsoTokenSource(Q_PRODUCT_NAME, "IamIdentityCenter",
                    loginParams.getLoginIdcParams().getUrl(), loginParams.getLoginIdcParams().getRegion());
        } else {
            source = new GetSsoTokenSource(Q_PRODUCT_NAME, "AwsBuilderId", null, null);
        }
        GetSsoTokenOptions options = new GetSsoTokenOptions(true, true, triggerSignIn);
        GetSsoTokenParams params = new GetSsoTokenParams(source, Q_SCOPES, options);
        return lspProvider.getAuthServer()
                           .thenCompose(server -> server.getSsoToken(params)
                                                        .thenApply(response -> {
                                                            if (triggerSignIn) {
                                                                LoginDetails loginDetails = new LoginDetails();
                                                                loginDetails.setIsLoggedIn(true);
                                                                loginDetails.setLoginType(currentLogin);
                                                                AuthStatusProvider.notifyAuthStatusChanged(loginDetails);
                                                            }
                                                            return response.ssoToken();
                                                        }))
                           .exceptionally(throwable -> {
                               Activator.getLogger().error("Failed to fetch SSO token from LSP", throwable);
                               throw new AmazonQPluginException(throwable);
                           });
    }
}
