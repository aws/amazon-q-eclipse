// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.providers.lsp;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.eclipse.lsp4j.services.LanguageServer;
import software.aws.toolkits.eclipse.amazonq.broker.events.AmazonQLspState;
import software.aws.toolkits.eclipse.amazonq.chat.ChatCommunicationManager;
import software.aws.toolkits.eclipse.amazonq.lsp.AmazonQLspServer;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.fetcher.RecordLspSetupArgs;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.telemetry.LanguageServerTelemetryProvider;
import software.aws.toolkits.telemetry.TelemetryDefinitions.Result;

public final class LspProviderImpl implements LspProvider {
    private static final LspProviderImpl INSTANCE = new LspProviderImpl();
    private static final long TIMEOUT_SECONDS = 60L;

    private final Map<Class<? extends LanguageServer>, ServerEntry> serverRegistry;

    private LspProviderImpl() {
        this.serverRegistry = new ConcurrentHashMap<>();
    }

    public static LspProvider getInstance() {
        return INSTANCE;
    }

    @Override
    public <T extends LanguageServer> void setServer(final Class<T> lspType, final T server) {
        synchronized (lspType) {
            ServerEntry entry = serverRegistry.computeIfAbsent(lspType, k -> new ServerEntry());
            entry.setServer(server);
        }
    }

    @Override
    public <T extends LanguageServer> void activate(final Class<T> lspType) {
        synchronized (AmazonQLspServer.class) {
            ServerEntry entry = serverRegistry.get(AmazonQLspServer.class);
            if (entry != null && entry.getFuture() != null) {
                entry.getFuture().complete(serverRegistry.get(lspType).getServer());
                onServerActivation();
            }
        }
    }

    @Override
    public CompletableFuture<AmazonQLspServer> getAmazonQServer() {
        return getServer(AmazonQLspServer.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends LanguageServer> CompletableFuture<T> getServer(final Class<T> lspType) {
        ServerEntry entry = serverRegistry.computeIfAbsent(lspType, k -> new ServerEntry());
        if (entry.getServer() != null) {
            return CompletableFuture.completedFuture((T) entry.getServer());
        }
        return entry.getFutureWithTimeout(TIMEOUT_SECONDS);
    }

    private void onServerActivation() {
        emitInitializeMetric();
        Activator.getEventBroker().post(AmazonQLspState.class, AmazonQLspState.ACTIVE);
        ChatCommunicationManager.getInstance();
    }

    private void emitInitializeMetric() {
        LanguageServerTelemetryProvider.emitSetupInitialize(Result.SUCCEEDED, new RecordLspSetupArgs());
    }

    private static final class ServerEntry {
        private LanguageServer server;
        private CompletableFuture<LanguageServer> future;

        public void setServer(final LanguageServer server) {
            this.server = server;
            if (future != null) {
                future.complete(server);
            }
        }

        public LanguageServer getServer() {
            return server;
        }

        public CompletableFuture<LanguageServer> getFuture() {
            return future;
        }

        @SuppressWarnings("unchecked")
        public <T extends LanguageServer> CompletableFuture<T> getFutureWithTimeout(final long timeoutSeconds) {
            if (future == null) {
                future = new CompletableFuture<>();
            }
            return future.orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                        .thenApply(server -> (T) server);
        }
    }
}
