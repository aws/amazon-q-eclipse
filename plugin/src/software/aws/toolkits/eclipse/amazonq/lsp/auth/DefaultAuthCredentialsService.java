// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.auth;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.jsonrpc.messages.ResponseMessage;

import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.lsp.encryption.LspEncryptionManager;
import software.aws.toolkits.eclipse.amazonq.lsp.model.BearerCredentials;
import software.aws.toolkits.eclipse.amazonq.lsp.model.UpdateCredentialsPayload;
import software.aws.toolkits.eclipse.amazonq.lsp.model.UpdateCredentialsPayloadData;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.providers.LspProvider;

public final class DefaultAuthCredentialsService implements AuthCredentialsService {
    private LspProvider lspProvider;
    private LspEncryptionManager encryptionManager;

    private DefaultAuthCredentialsService(final Builder builder) {
        this.lspProvider = Objects.requireNonNull(builder.lspProvider, "lspProvider must not be null");
        this.encryptionManager = Objects.requireNonNull(builder.encryptionManager, "encryptionManager must not be null");
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public CompletableFuture<ResponseMessage> updateTokenCredentials(final String accessToken, final boolean isEncrypted) {
        String token = accessToken;
        if (isEncrypted) {
            token = decryptSsoToken(accessToken);
        }
        UpdateCredentialsPayload payload = createUpdateCredentialsPayload(token);
        return lspProvider.getAmazonQServer()
                .thenCompose(server -> server.updateTokenCredentials(payload))
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

    public static class Builder {
        private LspProvider lspProvider;
        private LspEncryptionManager encryptionManager;

        public final Builder withLspProvider(final LspProvider lspProvider) {
            this.lspProvider = lspProvider;
            return this;
        }
        public final Builder withEncryptionManager(final LspEncryptionManager encryptionManager) {
            this.encryptionManager = encryptionManager;
            return this;
        }

        public final DefaultAuthCredentialsService build() {
            if (lspProvider == null) {
                lspProvider = Activator.getLspProvider();
            }
            DefaultAuthCredentialsService instance = new DefaultAuthCredentialsService(this);
            return instance;
        }
    }
}
