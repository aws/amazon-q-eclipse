// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.providers;

import software.aws.toolkits.eclipse.amazonq.lsp.manager.DefaultLspManager;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspConstants;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspManager;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.fetcher.RemoteManifestLspFetcher;

public class LspManagerProvider {

    private LspManagerProvider() {
        // prevent instantiation
    }

    private static LspManager instance;

    public static LspManager getInstance() {
        if (instance == null) {
            synchronized (LspManagerProvider.class) {
                if (instance == null) {
                    instance = createLspManager();
                }
            }
        }
        return instance;
    }

    private static LspManager createLspManager() {
        return DefaultLspManager.builder()
            .withLspExecutablePrefix(LspConstants.CW_LSP_FILENAME)
            .withFetcher(RemoteManifestLspFetcher.builder()
                .withManifestUrl(LspConstants.CW_MANIFEST_URL)
                .build())
            .build();
    }

}