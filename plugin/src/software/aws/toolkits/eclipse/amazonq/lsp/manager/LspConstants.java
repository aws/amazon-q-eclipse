// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.manager;

import org.eclipse.osgi.service.resolver.VersionRange;

public final class LspConstants {
    private LspConstants() {
        // Prevent instantiation
    }

    public static final String CW_MANIFEST_URL = "https://d3akiidp1wvqyg.cloudfront.net/eclipse/0/manifest.json";
    public static final int MANIFEST_MAJOR_VERSION = 0;

    public static final String CW_LSP_FILENAME = "aws-lsp-codewhisperer.js";
    public static final String NODE_EXECUTABLE_PREFIX = "node";

    public static final String LSP_SUBDIRECTORY = "lsp";

    // TODO: constrain these prior to launch
    public static final VersionRange LSP_SUPPORTED_VERSION_RANGE = new VersionRange("[0.0.0, 10.0.0]");
}
