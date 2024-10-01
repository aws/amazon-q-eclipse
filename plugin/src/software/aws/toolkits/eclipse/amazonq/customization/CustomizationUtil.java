// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.customization;

import java.util.Map;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.providers.LspProvider;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;

public final class CustomizationUtil {

    private CustomizationUtil() {
        // to avoid initiation
    }

    public static void triggerChangeConfigurationNotification(final Map<String, Object> settings) {
        try {
            PluginLogger.info("Sending configuration update notification to Amazon Q LSP server");
            LspProvider.getAmazonQServer()
            .thenAccept(server -> server.getWorkspaceService().didChangeConfiguration(new DidChangeConfigurationParams(settings)));
        } catch (Exception e) {
            PluginLogger.error("Error occurred while sending change configuration notification to Amazon Q LSP server", e);
            throw new AmazonQPluginException(e);
        }
    }

}
