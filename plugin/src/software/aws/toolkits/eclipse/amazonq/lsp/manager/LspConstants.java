// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.manager;

import java.nio.file.Paths;

import org.eclipse.osgi.service.resolver.VersionRange;

public final class LspConstants {
    private LspConstants() {
        // Prevent instantiation
    }

    public static final String CW_MANIFEST_URL = "https://d3akiidp1wvqyg.cloudfront.net/codewhisperer/0/manifest.json";
    public static final int MANIFEST_MAJOR_VERSION = 0;

    public static final String CW_LSP_FILENAME = "aws-lsp-codewhisperer.js";
    public static final String NODE_EXECUTABLE_WINDOWS = "node.exe";
    public static final String NODE_EXECUTABLE_OSX = "node";
    public static final String CHAT_UI_FILENAME = "amazonq-ui.js";
    public static final String LSP_CLIENT_FOLDER = "clients";
    public static final String LSP_SERVER_FOLDER = "servers";

    public static final String LSP_SUBDIRECTORY = "lsp";
    public static final String AMAZONQ_LSP_SUBDIRECTORY = Paths.get(LSP_SUBDIRECTORY, "AmazonQ").toString();

    public static final VersionRange LSP_SUPPORTED_VERSION_RANGE = new VersionRange("[2.2.0, 2.10.0)");
}
