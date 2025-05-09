// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.providers.lsp;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.eclipse.lsp4j.services.LanguageServer;

import software.aws.toolkits.eclipse.amazonq.broker.events.AmazonQLspState;
import software.aws.toolkits.eclipse.amazonq.lsp.AmazonQLspServer;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.fetcher.RecordLspSetupArgs;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.telemetry.LanguageServerTelemetryProvider;
import software.aws.toolkits.telemetry.TelemetryDefinitions.Result;

public final class LspProviderImpl implements LspProvider {
    private static final LspProviderImpl INSTANCE = new LspProviderImpl();

    private static final long TIMEOUT_SECONDS = 60L;

    private final Map<Class<? extends LanguageServer>, CompletableFuture<LanguageServer>> futures;
    private final Map<Class<? extends LanguageServer>, LanguageServer> servers;

    private LspProviderImpl() {
        this.futures = new ConcurrentHashMap<>();
        this.servers = new ConcurrentHashMap<>();
    }

    public static LspProvider getInstance() {
        return INSTANCE;
    }

    @Override
    public <T extends LanguageServer> void setServer(final Class<T> lspType, final T server) {
        synchronized (lspType) {
            servers.put(lspType, server);
            CompletableFuture<LanguageServer> future = futures.remove(lspType);
            if (future != null) {
                future.complete(server);
            }
        }
    }

    @Override
    public void setAmazonQServer(final LanguageServer server) {
        synchronized (AmazonQLspServer.class) {
            servers.put(AmazonQLspServer.class, server);
            CompletableFuture<LanguageServer> future = futures.remove(AmazonQLspServer.class);
            if (future != null) {
                future.complete(server);
            }
            emitInitializeMetric();
            Activator.getEventBroker().post(AmazonQLspState.class, AmazonQLspState.ACTIVE);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends LanguageServer> CompletableFuture<T> getServer(final Class<T> lspType) {
        synchronized (lspType) {
            T server = (T) servers.get(lspType);
            if (server != null) {
                return CompletableFuture.completedFuture(server);
            }

            CompletableFuture<LanguageServer> future = futures.computeIfAbsent(lspType, k -> new CompletableFuture<>());
            return future.orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                          .thenApply(lspServer -> (T) lspServer);
        }
    }

    @Override
    public CompletableFuture<AmazonQLspServer> getAmazonQServer() {
        return getServer(AmazonQLspServer.class);
    }

    private void emitInitializeMetric() {
        LanguageServerTelemetryProvider.emitSetupInitialize(Result.SUCCEEDED, new RecordLspSetupArgs());
    }

}
