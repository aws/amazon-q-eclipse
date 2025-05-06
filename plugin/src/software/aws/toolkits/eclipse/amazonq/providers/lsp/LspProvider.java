// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.providers.lsp;

import org.eclipse.lsp4j.services.LanguageServer;

import software.aws.toolkits.eclipse.amazonq.lsp.AmazonQLspServer;

import java.util.concurrent.CompletableFuture;

/**
 * Provides management of Language Server Protocol (LSP) servers.
 */
public interface LspProvider {
    /**
     * Sets a language server of the specified type.
     *
     * @param <T> The type of language server
     * @param lspType The class of the language server
     * @param server The server instance
     */
    <T extends LanguageServer> void setServer(Class<T> lspType, T server);

    /**
     * Gets a language server of the specified type.
     *
     * @param <T> The type of language server
     * @param lspType The class of the language server
     * @return A future that completes with the specified server
     */
    <T extends LanguageServer> CompletableFuture<T> getServer(Class<T> lspType);

    /**
     * Activates a language server of the specified type.
     *
     * @param <T> The type of language server
     * @param lspType The class of the language server to activate
     */
    <T extends LanguageServer> void activate(Class<T> lspType);

    /**
     * Gets the Amazon Q language server.
     *
     * @return A future that completes with the Amazon Q server
     */
    CompletableFuture<AmazonQLspServer> getAmazonQServer();
}
