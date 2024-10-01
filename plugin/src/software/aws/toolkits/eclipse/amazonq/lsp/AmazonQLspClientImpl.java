// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4e.LanguageClientImpl;
import org.eclipse.lsp4j.ConfigurationParams;

import software.aws.toolkits.eclipse.amazonq.configuration.PluginStore;
import software.aws.toolkits.eclipse.amazonq.lsp.model.ConnectionMetadata;
import software.aws.toolkits.eclipse.amazonq.lsp.model.SsoProfileData;
import software.aws.toolkits.eclipse.amazonq.util.Constants;

@SuppressWarnings("restriction")
public class AmazonQLspClientImpl extends LanguageClientImpl implements AmazonQLspClient {

    @Override
    public final CompletableFuture<ConnectionMetadata> getConnectionMetadata() {
        // TODO don't hardcode start URL
        SsoProfileData sso = new SsoProfileData();
        sso.setStartUrl("https://view.awsapps.com/start");
        ConnectionMetadata metadata = new ConnectionMetadata();
        metadata.setSso(sso);
        return CompletableFuture.completedFuture(metadata);
    }

    @Override
    public final CompletableFuture<List<Object>> configuration(final ConfigurationParams configurationParams) {
        if (configurationParams.getItems().size() == 0) {
            return CompletableFuture.completedFuture(null);
        }
        List<Object> output = new ArrayList<>();
        configurationParams.getItems().forEach(item -> {
            if (item.getSection().equals(Constants.LSP_CONFIGURATION_KEY)) {
                String customizationArn = PluginStore.get(Constants.CUSTOMIZATION_STORAGE_INTERNAL_KEY);
                Map<String, String> customization = new HashMap<>();
                customization.put(Constants.LSP_CUSTOMIZATION_CONFIGURATION_KEY, customizationArn);
                output.add(customization);
            } else {
                output.add(null);
            }
        });
        return CompletableFuture.completedFuture(output);
    }

}
