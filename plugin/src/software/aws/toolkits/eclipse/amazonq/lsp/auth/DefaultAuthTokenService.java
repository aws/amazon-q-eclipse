// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.auth;

import static software.aws.toolkits.eclipse.amazonq.util.QConstants.Q_SCOPES;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import software.amazon.awssdk.services.toolkittelemetry.model.AWSProduct;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.GetSsoTokenOptions;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.GetSsoTokenParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.GetSsoTokenSource;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.InvalidateSsoTokenParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.InvalidateSsoTokenResult;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginParams;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.Profile;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.ProfileSettings;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.SsoSession;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.SsoSessionSettings;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.SsoToken;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.UpdateProfileOptions;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.UpdateProfileParams;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.providers.LspProvider;
import software.aws.toolkits.eclipse.amazonq.util.Constants;

public final class DefaultAuthTokenService implements AuthTokenService {
    private LspProvider lspProvider;

    private DefaultAuthTokenService(final Builder builder) {
        this.lspProvider = Objects.requireNonNull(builder.lspProvider, "lspProvider cannot be null");
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public CompletableFuture<SsoToken> getSsoToken(final LoginType loginType, final LoginParams loginParams,
                final boolean loginOnInvalidToken) {
        GetSsoTokenParams getSsoTokenParams = createGetSsoTokenParams(loginType, loginOnInvalidToken);
        return lspProvider.getAmazonQServer()
                .thenApply(server -> {
                    if (loginOnInvalidToken && loginType.equals(LoginType.IAM_IDENTITY_CENTER)) {
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
                            throw new AmazonQPluginException("Failed to update profile", e);
                        }
                    }
                    return server;
                })
                .thenCompose(server -> server.getSsoToken(getSsoTokenParams))
                .thenApply(response -> {
                    return response.ssoToken();
                })
                .exceptionally(throwable -> {
                    throw new AmazonQPluginException("Failed to fetch SSO token", throwable);
                });
    }

    @Override
    public CompletableFuture<InvalidateSsoTokenResult> invalidateSsoToken(final InvalidateSsoTokenParams invalidateSsoTokenParams) {
        return lspProvider.getAmazonQServer()
                .thenCompose(server -> server.invalidateSsoToken(invalidateSsoTokenParams))
                .exceptionally(throwable -> {
                    throw new AmazonQPluginException("Failed to invalidate SSO token", throwable);
                });
    }

    private GetSsoTokenParams createGetSsoTokenParams(final LoginType currentLogin, final boolean loginOnInvalidToken) {
        GetSsoTokenSource source = currentLogin.equals(LoginType.IAM_IDENTITY_CENTER)
                ? new GetSsoTokenSource(LoginType.IAM_IDENTITY_CENTER.getValue(), null, Constants.IDC_PROFILE_NAME)
                : new GetSsoTokenSource(LoginType.BUILDER_ID.getValue(), Q_SCOPES, null);
        GetSsoTokenOptions options = new GetSsoTokenOptions(loginOnInvalidToken);
        return new GetSsoTokenParams(source, AWSProduct.AMAZON_Q_FOR_ECLIPSE.toString(), options);
    }

    public static class Builder {
        private LspProvider lspProvider;

        public final Builder withLspProvider(final LspProvider lspProvider) {
            this.lspProvider = lspProvider;
            return this;
        }

        public final DefaultAuthTokenService build() {
            if (lspProvider == null) {
                lspProvider = Activator.getLspProvider();
            }
            DefaultAuthTokenService instance = new DefaultAuthTokenService(this);
            return instance;
        }
    }
}
