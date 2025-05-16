// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.configuration.customization;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.mylyn.commons.ui.dialogs.AbstractNotificationPopup;
import org.eclipse.swt.widgets.Display;

import software.amazon.awssdk.utils.StringUtils;
import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.lsp.model.GetConfigurationFromServerParams;
import software.aws.toolkits.eclipse.amazonq.lsp.model.GetConfigurationFromServerParams.ExpectedResponseType;
import software.aws.toolkits.eclipse.amazonq.lsp.model.LspServerConfigurations;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.Constants;
import software.aws.toolkits.eclipse.amazonq.util.ToolkitNotification;
import software.aws.toolkits.eclipse.amazonq.views.model.Customization;

public final class CustomizationUtil {

    private CustomizationUtil() {
        // to avoid initiation
    }

    public static void triggerChangeConfigurationNotification() {
        try {
            Activator.getLogger().info("Triggering configuration pull from Amazon Q LSP server");
            Activator.getLspProvider().getAmazonQServer()
                    .thenAccept(server -> {
                        server.getWorkspaceService().didChangeConfiguration(new DidChangeConfigurationParams());
                    }).get();
        } catch (Exception e) {
            Activator.getLogger().error("Error occurred while sending change configuration notification to Amazon Q LSP server", e);
            throw new AmazonQPluginException(e);
        }
    }

    public static CompletableFuture<List<Customization>> listCustomizations() {
        GetConfigurationFromServerParams params = new GetConfigurationFromServerParams(
                ExpectedResponseType.CUSTOMIZATION);
        return Activator.getLspProvider().getAmazonQServer()
                .thenCompose(server -> {
                    CompletableFuture<LspServerConfigurations<Customization>> config = server
                            .getConfigurationFromServer(params);
                    return config;
                })
                .thenApply(configurations -> Optional.ofNullable(configurations)
                        .map(config -> config.getConfigurations().stream()
                            .filter(customization -> customization != null && StringUtils.isNotBlank(customization.getName()))
                            .collect(Collectors.toList()))
                        .orElse(Collections.emptyList()))
                .exceptionally(throwable -> {
                    Activator.getLogger().error("Error occurred while fetching the list of customizations", throwable);
                    throw new AmazonQPluginException(throwable);
               });
    }

    public static void validateCurrentCustomization() {
        listCustomizations().thenAccept(customizations -> {
            Customization currentCustomization = Activator.getPluginStore()
                    .getObject(Constants.CUSTOMIZATION_STORAGE_INTERNAL_KEY, Customization.class);

            for (final Customization validCustomization : customizations) {
                if (validCustomization.getArn().equals(currentCustomization.getArn())) {
                    return;
                }
            }

            // Use default customization
            Activator.getPluginStore().remove(Constants.CUSTOMIZATION_STORAGE_INTERNAL_KEY);
            Display.getDefault()
                    .asyncExec(() -> CustomizationUtil.showNotification(Constants.DEFAULT_Q_FOUNDATION_DISPLAY_NAME));
        });
    }

    public static void showNotification(final String customizationName) {
        AbstractNotificationPopup notification = new ToolkitNotification(Display.getCurrent(),
                Constants.IDE_CUSTOMIZATION_NOTIFICATION_TITLE,
                String.format(Constants.IDE_CUSTOMIZATION_NOTIFICATION_BODY_TEMPLATE, customizationName));
        notification.open();
    }

}
