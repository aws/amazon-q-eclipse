// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.manager.fetcher;

import java.nio.file.Path;

import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspFetchResult;
import software.aws.toolkits.eclipse.amazonq.util.PluginArchitecture;
import software.aws.toolkits.eclipse.amazonq.util.PluginPlatform;

public interface LspFetcher {
    LspFetchResult fetch(PluginPlatform platform, PluginArchitecture architecture, Path destination);
}
