// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.router;

import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspState;
import software.aws.toolkits.eclipse.amazonq.providers.browser.BrowserCompatibilityState;

public record PluginState(AuthState authState, LspState lspState, BrowserCompatibilityState browserCompatibilityState) {
}
