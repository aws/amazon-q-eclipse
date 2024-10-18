// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp;

import org.eclipse.lsp4e.LanguageClientImpl;
import org.eclipse.lsp4j.ShowDocumentParams;
import org.eclipse.lsp4j.ShowDocumentResult;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import software.aws.toolkits.eclipse.amazonq.lsp.model.ConnectionMetadata;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("restriction")
public class AuthLspClientImpl extends LanguageClientImpl implements AmazonQLspClient {

    @Override
    public final CompletableFuture<ConnectionMetadata> getConnectionMetadata() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public final CompletableFuture<ShowDocumentResult> showDocument(final ShowDocumentParams params) {
        Activator.getLogger().info("Opening redirect URL: " + params.getUri());
        return CompletableFuture.supplyAsync(() -> {
            try {
                PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(params.getUri()));
                return new ShowDocumentResult(true);
            } catch (PartInitException | MalformedURLException e) {
                Activator.getLogger().error("Error opening URL: " + params.getUri(), e);
                return new ShowDocumentResult(false);
            }
        });
    }
}
