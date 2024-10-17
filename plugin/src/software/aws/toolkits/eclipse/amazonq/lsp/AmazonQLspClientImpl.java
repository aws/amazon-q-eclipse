// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4e.LanguageClientImpl;
import org.eclipse.lsp4j.ConfigurationParams;
import org.eclipse.lsp4j.ProgressParams;

import software.amazon.awssdk.services.toolkittelemetry.model.Sentiment;
import software.aws.toolkits.eclipse.amazonq.chat.ChatCommunicationManager;
import software.aws.toolkits.eclipse.amazonq.configuration.PluginStore;
import software.aws.toolkits.eclipse.amazonq.lsp.model.ConnectionMetadata;
import software.aws.toolkits.eclipse.amazonq.lsp.model.SsoProfileData;
import software.aws.toolkits.eclipse.amazonq.lsp.model.TelemetryEvent;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.preferences.AmazonQPreferencePage;
import software.aws.toolkits.eclipse.amazonq.util.Constants;
import software.aws.toolkits.eclipse.amazonq.util.ObjectMapperFactory;
import software.aws.toolkits.eclipse.amazonq.views.model.Customization;
import software.aws.toolkits.eclipse.amazonq.util.ThreadingUtils;

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
            if (item.getSection().equals(Constants.LSP_Q_CONFIGURATION_KEY)) {
                Customization storedCustomization = PluginStore.getObject(Constants.CUSTOMIZATION_STORAGE_INTERNAL_KEY, Customization.class);
                Map<String, String> customization = new HashMap<>();
                customization.put(Constants.LSP_CUSTOMIZATION_CONFIGURATION_KEY, Objects.nonNull(storedCustomization) ? storedCustomization.getArn() : null);
                output.add(customization);
            } else if (item.getSection().equals(Constants.LSP_CW_CONFIGURATION_KEY)) {
                Map<String, Boolean> cwConfig = new HashMap<>();
                boolean shareContentSetting = Activator.getDefault().getPreferenceStore().getBoolean(AmazonQPreferencePage.Q_DATA_SHARING);
                cwConfig.put(Constants.LSP_CW_OPT_OUT_KEY, shareContentSetting);
                output.add(cwConfig);
            }
        });
        return CompletableFuture.completedFuture(output);
    }

    /*
     * Handles the progress notifications received from the LSP server.
     * - Process partial results for Chat messages if provided token is maintained by ChatCommunicationManager
     * - Other notifications are ignored at this time.
     */
    @Override
    public final void notifyProgress(final ProgressParams params) {
        var chatCommunicationManager = ChatCommunicationManager.getInstance();

        ThreadingUtils.executeAsyncTask(() -> {
            try {
                chatCommunicationManager.handlePartialResultProgressNotification(params);
            } catch (Exception e) {
                Activator.getLogger().error("Error processing partial result progress notification", e);
            }
        });
    }

    @Override
    public final void telemetryEvent(final Object event) {
        TelemetryEvent telemetryEvent = ObjectMapperFactory.getInstance().convertValue(event, TelemetryEvent.class);
        switch (telemetryEvent.name()) {
        // intercept the feedback telemetry event and re-route to our feedback backend
        case "amazonq_sendFeedback":
            sendFeedback(telemetryEvent);
            break;
        default:
            Activator.getTelemetryService().emitMetric(telemetryEvent);
        }
    }

    private void sendFeedback(final TelemetryEvent telemetryEvent) {
        var data = telemetryEvent.data();

        if (data.containsKey("sentiment")) {
            var sentiment = data.get("sentiment").equals(Sentiment.POSITIVE) ? Sentiment.POSITIVE : Sentiment.NEGATIVE;

            var comment = Optional.ofNullable(data.get("comment")).filter(String.class::isInstance)
                    .map(String.class::cast).orElse("");
            try {
                Activator.getTelemetryService().emitFeedback(comment, sentiment);
            } catch (Exception e) {
                Activator.getLogger().error("Error occurred when submitting feedback", e);
            }
        }
    }
}
