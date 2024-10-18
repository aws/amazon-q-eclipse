// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.customization;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.DidChangeConfigurationParams;

import software.amazon.awssdk.utils.StringUtils;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.lsp.model.GetConfigurationFromServerParams;
import software.aws.toolkits.eclipse.amazonq.providers.LspProvider;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.views.model.Customization;

public final class CustomizationUtil {

    private CustomizationUtil() {
        // to avoid initiation
    }

    public static void triggerChangeConfigurationNotification(final Map<String, Object> settings) {
        try {
            Activator.getLogger().info("Sending configuration update notification to Amazon Q LSP server");
            LspProvider.getAmazonQServer()
            .thenAccept(server -> server.getWorkspaceService().didChangeConfiguration(new DidChangeConfigurationParams(settings)));
        } catch (Exception e) {
            Activator.getLogger().error("Error occurred while sending change configuration notification to Amazon Q LSP server", e);
            throw new AmazonQPluginException(e);
        }
    }

    public static CompletableFuture<List<Customization>> listCustomizations() {
        GetConfigurationFromServerParams params = new GetConfigurationFromServerParams();
        params.setSection("aws.q");
        return LspProvider.getAmazonQServer()
                .thenCompose(server -> server.getConfigurationFromServer(params))
                .thenApply(configurations -> configurations.getCustomizations().stream()
                    .filter(customization -> customization != null && StringUtils.isNotBlank(customization.getName()))
                    .collect(Collectors.toList()))
                .exceptionally(throwable -> {
                    Activator.getLogger().error("Error occurred while fetching the list of customizations", throwable);
                    throw new AmazonQPluginException(throwable);
               });
    }
}
