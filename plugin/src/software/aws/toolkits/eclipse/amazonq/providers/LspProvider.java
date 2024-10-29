// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.providers;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.eclipse.lsp4j.services.LanguageServer;

import software.aws.toolkits.eclipse.amazonq.lsp.AmazonQLspServer;

public final class LspProvider {

    private LspProvider() {
        // Prevent instantiation
    }

    private static final long TIMEOUT_SECONDS = 10L;

    private static final Map<Class<? extends LanguageServer>, CompletableFuture<LanguageServer>> FUTURES = new ConcurrentHashMap<>();
    private static final Map<Class<? extends LanguageServer>, LanguageServer> SERVERS = new ConcurrentHashMap<>();

    public static <T extends LanguageServer> void setServer(final Class<T> lspType, final T server) {
        synchronized (lspType) {
            SERVERS.put(lspType, server);
            CompletableFuture<LanguageServer> future = FUTURES.remove(lspType);
            if (future != null) {
                future.complete(server);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends LanguageServer> CompletableFuture<T> getServer(final Class<T> lspType) {
        synchronized (lspType) {
            T server = (T) SERVERS.get(lspType);
            if (server != null) {
                return CompletableFuture.completedFuture(server);
            }

            CompletableFuture<LanguageServer> future = FUTURES.computeIfAbsent(lspType, k -> new CompletableFuture<>());
            return future.orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                          .thenApply(lspServer -> (T) lspServer);
        }
    }

    public static CompletableFuture<AmazonQLspServer> getAmazonQServer() {
        return getServer(AmazonQLspServer.class);
    }

}
