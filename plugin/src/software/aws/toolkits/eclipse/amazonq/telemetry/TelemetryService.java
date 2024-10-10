// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.telemetry;

import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentity.CognitoIdentityClient;
import software.amazon.awssdk.services.toolkittelemetry.ToolkitTelemetryClient;
import software.amazon.awssdk.services.toolkittelemetry.model.AWSProduct;
import software.amazon.awssdk.services.toolkittelemetry.model.MetadataEntry;
import software.amazon.awssdk.services.toolkittelemetry.model.MetricDatum;
import software.amazon.awssdk.services.toolkittelemetry.model.PostFeedbackRequest;
import software.amazon.awssdk.services.toolkittelemetry.model.PostMetricsRequest;
import software.amazon.awssdk.services.toolkittelemetry.model.Sentiment;
import software.amazon.awssdk.services.toolkittelemetry.model.Unit;
import software.aws.toolkits.eclipse.amazonq.lsp.model.TelemetryEvent;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.preferences.AmazonQPreferencePage;
import software.aws.toolkits.eclipse.amazonq.util.ClientMetadata;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

public final class TelemetryService {
    private static final Region TELEMETRY_REGION = Region.US_EAST_1;
    private static final String TELEMETRY_ENDPOINT = "https://7zftft3lj2.execute-api.us-east-1.amazonaws.com/Beta";
    private static final String TELEMETRY_IDENTITY_POOL = "us-east-1:db7bfc9f-8ecd-4fbb-bea7-280c16069a99";
    private static ToolkitTelemetryClient telemetryClient;

    // prevent instantiation
    private TelemetryService() { }

    public static ToolkitTelemetryClient getClient() {
        if (telemetryClient == null) {
            telemetryClient = createDefaultTelemetryClient();
        }
        return telemetryClient;
    }

    public static void emitMetric(final TelemetryEvent event) {
        if (!telemetryEnabled()) {
            return;
        }

        List<MetadataEntry> metadataEntries = new ArrayList<>();
        for (Map.Entry<String, Object> entry : event.data().entrySet()) {
            MetadataEntry.Builder builder = MetadataEntry.builder();
            builder.key(entry.getKey());
            builder.value(entry.getValue().toString());
            metadataEntries.add(builder.build());
        }
        MetricDatum datum = MetricDatum.builder()
                .metricName(event.name())
                .epochTimestamp(Instant.now().toEpochMilli())
                .value(1.0)
                .passive(false)
                .unit(Unit.NONE)
                .metadata(metadataEntries)
                .build();
        emitMetric(datum);
    }

    public static void emitMetric(final MetricDatum datum) {
        if (!telemetryEnabled()) {
            return;
        }

        getClient().postMetrics(PostMetricsRequest.builder()
                .awsProduct(AWSProduct.AWS_TOOLKIT_FOR_ECLIPSE)
                .awsProductVersion(ClientMetadata.getPluginVersion())
                .clientID(ClientMetadata.getClientId())
                .parentProduct(ClientMetadata.getIdeName())
                .parentProductVersion(ClientMetadata.getIdeVersion())
                .os(ClientMetadata.getOSName())
                .osVersion(ClientMetadata.getOSVersion())
                .metricData(datum)
                .build());
    }

    public static void emitFeedback(final String comment, final Sentiment sentiment) {
        getClient().postFeedback(PostFeedbackRequest.builder()
                .awsProduct(AWSProduct.AWS_TOOLKIT_FOR_ECLIPSE)
                .awsProductVersion(ClientMetadata.getPluginVersion())
                .parentProduct(ClientMetadata.getIdeName())
                .parentProductVersion(ClientMetadata.getIdeVersion())
                .os(ClientMetadata.getOSName())
                .osVersion(ClientMetadata.getOSVersion())
                .comment(comment)
                .sentiment(sentiment)
                .build());
    }

    private static ClientOverrideConfiguration.Builder nullDefaultProfileFile(final ClientOverrideConfiguration.Builder builder) {
        return builder.defaultProfileFile(ProfileFile.builder()
                .content(InputStream.nullInputStream())
                .type(ProfileFile.Type.CONFIGURATION)
                .build());
    }

    private static ToolkitTelemetryClient createDefaultTelemetryClient() {
        CognitoIdentityClient cognitoClient = CognitoIdentityClient.builder()
                .credentialsProvider(AnonymousCredentialsProvider.create())
                .region(TELEMETRY_REGION)
                .httpClient(ApacheHttpClient.create())
                .overrideConfiguration(builder -> nullDefaultProfileFile(builder))
                .build();
        AwsCredentialsProvider credentialsProvider = new AwsCognitoCredentialsProvider(TELEMETRY_IDENTITY_POOL, cognitoClient);
        return ToolkitTelemetryClient.builder()
                .region(TELEMETRY_REGION)
                .credentialsProvider(credentialsProvider)
                .endpointOverride(URI.create(TELEMETRY_ENDPOINT))
                .build();
    }

    private static boolean telemetryEnabled() {
        return Activator.getDefault().getPreferenceStore().getBoolean(AmazonQPreferencePage.TELEMETRY_OPT_IN);
    }
}
