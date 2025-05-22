// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.auth;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.lsp.model.UpdateCredentialsPayload;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.providers.lsp.LspProvider;

public final class DefaultAuthCredentialsService implements AuthCredentialsService {
    private LspProvider lspProvider;

    private DefaultAuthCredentialsService(final Builder builder) {
        this.lspProvider = Objects.requireNonNull(builder.lspProvider, "lspProvider must not be null");
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public CompletableFuture<Void> updateTokenCredentials(final UpdateCredentialsPayload params) {
        return lspProvider.getAmazonQServer()
                .thenCompose(server -> server.updateTokenCredentials(params))
                .exceptionally(throwable -> {
                    throw new AmazonQPluginException("Failed to update token credentials", throwable);
                });
    }

    @Override
    public CompletableFuture<Void> deleteTokenCredentials() {
        return lspProvider.getAmazonQServer()
                .thenAccept(server -> server.deleteTokenCredentials())
                .exceptionally(throwable -> {
                    throw new AmazonQPluginException("Failed to delete token credentials", throwable);
                });
    }

    public static final class Builder {
        private LspProvider lspProvider;

        public Builder withLspProvider(final LspProvider lspProvider) {
            this.lspProvider = lspProvider;
            return this;
        }

        public DefaultAuthCredentialsService build() {
            if (lspProvider == null) {
                lspProvider = Activator.getLspProvider();
            }
            DefaultAuthCredentialsService instance = new DefaultAuthCredentialsService(this);
            return instance;
        }
    }
}
