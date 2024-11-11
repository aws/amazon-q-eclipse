// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.providers;

import org.eclipse.lsp4j.services.LanguageServer;
import java.util.concurrent.CompletableFuture;
import software.aws.toolkits.eclipse.amazonq.lsp.AmazonQLspServer;

public interface LspProvider {
    <T extends LanguageServer> void setServer(Class<T> lspType, T server);
    CompletableFuture<AmazonQLspServer> getAmazonQServer();
}

