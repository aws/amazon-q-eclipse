// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.manager;

import software.aws.toolkits.eclipse.amazonq.lsp.model.LanguageServerLocation;

public record LspFetchResult(String assetDirectory, String version, LanguageServerLocation location) { }
