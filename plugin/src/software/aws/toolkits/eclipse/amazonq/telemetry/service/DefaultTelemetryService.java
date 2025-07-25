// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.telemetry.service;

import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.net.ssl.SSLContext;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.SystemDefaultCredentialsProvider;

import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache.ProxyConfiguration;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentity.CognitoIdentityClient;
import software.amazon.awssdk.services.toolkittelemetry.ToolkitTelemetryClient;
import software.amazon.awssdk.services.toolkittelemetry.model.MetadataEntry;
import software.amazon.awssdk.services.toolkittelemetry.model.MetricDatum;
import software.amazon.awssdk.services.toolkittelemetry.model.PostFeedbackRequest;
import software.amazon.awssdk.services.toolkittelemetry.model.PostMetricsRequest;
import software.amazon.awssdk.services.toolkittelemetry.model.Sentiment;
import software.amazon.awssdk.services.toolkittelemetry.model.Unit;
import software.amazon.awssdk.utils.StringUtils;
import software.aws.toolkits.eclipse.amazonq.lsp.model.TelemetryEvent;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.preferences.AmazonQPreferencePage;
import software.aws.toolkits.eclipse.amazonq.telemetry.AwsCognitoCredentialsProvider;
import software.aws.toolkits.eclipse.amazonq.telemetry.metadata.ClientMetadata;
import software.aws.toolkits.eclipse.amazonq.telemetry.metadata.PluginClientMetadata;
import software.aws.toolkits.eclipse.amazonq.util.ProxyUtil;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

public final class DefaultTelemetryService implements TelemetryService {
    private ToolkitTelemetryClient telemetryClient;
    private ClientMetadata clientMetadata;

    private DefaultTelemetryService(final Builder builder) {
        this.telemetryClient = Objects.requireNonNull(builder.telemetryClient, "telemetry client cannot be null");
        this.clientMetadata = Objects.requireNonNull(builder.clientMetadata, "client metadata cannot be null");
    }

    public static Builder builder() {
        return new Builder();
    }

    public void emitMetric(final TelemetryEvent event) {
        if (!telemetryEnabled()) {
            return;
        }
        List<MetadataEntry> metadataEntries = new ArrayList<>();
        addMetadata("result", event.result(), metadataEntries);
        for (Map.Entry<String, Object> entry : event.data().entrySet()) {
            addMetadata(entry.getKey(), entry.getValue(), metadataEntries);
        }
        if (event.errorData() != null) {
            addMetadata("reason", event.errorData().reason(), metadataEntries);
            addMetadata("errorCode", event.errorData().errorCode(), metadataEntries);
            addMetadata("httpStatusCode", event.errorData().httpStatusCode(), metadataEntries);
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

    public void emitMetric(final MetricDatum datum) {
        if (!telemetryEnabled()) {
            return;
        }

        try {
            telemetryClient.postMetrics(PostMetricsRequest.builder()
                    .awsProduct(clientMetadata.getPluginName())
                    .awsProductVersion(clientMetadata.getPluginVersion())
                    .clientID(clientMetadata.getClientId())
                    .parentProduct(clientMetadata.getIdeName())
                    .parentProductVersion(clientMetadata.getIdeVersion())
                    .os(clientMetadata.getOSName())
                    .osVersion(clientMetadata.getOSVersion())
                    .metricData(datum)
                    .build());
        } catch (Exception e) {
            Activator.getLogger().warn("Unable to emit telemetry: ", e);
        }
    }

    public void emitFeedback(final String comment, final Sentiment sentiment) {
        try {
            telemetryClient.postFeedback(PostFeedbackRequest.builder()
                    .awsProduct(clientMetadata.getPluginName())
                    .awsProductVersion(clientMetadata.getPluginVersion())
                    .parentProduct(clientMetadata.getIdeName())
                    .parentProductVersion(clientMetadata.getIdeVersion())
                    .os(clientMetadata.getOSName())
                    .osVersion(clientMetadata.getOSVersion())
                    .comment(comment)
                    .sentiment(sentiment)
                    .build());
        } catch (Exception e) {
            Activator.getLogger().warn("Unable to send feedback: ", e);
        }
    }

    private static ClientOverrideConfiguration.Builder nullDefaultProfileFile(final ClientOverrideConfiguration.Builder builder) {
        return builder.defaultProfileFile(ProfileFile.builder()
                .content(InputStream.nullInputStream())
                .type(ProfileFile.Type.CONFIGURATION)
                .build());
    }

    private static ToolkitTelemetryClient createDefaultTelemetryClient(final Region region, final String endpoint, final String identityPool) {
        SSLContext sslContext = ProxyUtil.getCustomSslContext();
        if (sslContext == null) {
            try {
                sslContext = SSLContext.getInstance("TLSv1.2");
                sslContext.init(null, null, null);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create SSLContext for TLS 1.2", e);
            }
        }

        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
            sslContext,
            new String[]{"TLSv1.2"},
            null,
            SSLConnectionSocketFactory.getDefaultHostnameVerifier()
        );
        var proxyUrl = ProxyUtil.getHttpsProxyUrlForEndpoint(endpoint);
        var httpClientBuilder = ApacheHttpClient.builder();
        if (!StringUtils.isEmpty(proxyUrl)) {
            httpClientBuilder.proxyConfiguration(ProxyConfiguration.builder()
                    .endpoint(URI.create(proxyUrl))
                    .build());
        }

        httpClientBuilder.socketFactory(sslSocketFactory);

        SdkHttpClient sdkHttpClient = httpClientBuilder
                .credentialsProvider(new SystemDefaultCredentialsProvider())
                .build();
        CognitoIdentityClient cognitoClient = CognitoIdentityClient.builder()
                .credentialsProvider(AnonymousCredentialsProvider.create())
                .region(region)
                .httpClient(sdkHttpClient)
                .overrideConfiguration(builder -> {
                    nullDefaultProfileFile(builder);
                    builder.retryStrategy(RetryMode.STANDARD);
                }).build();
        AwsCredentialsProvider credentialsProvider = new AwsCognitoCredentialsProvider(identityPool, cognitoClient);
        return ToolkitTelemetryClient.builder()
                .region(region)
                .httpClient(sdkHttpClient)
                .credentialsProvider(credentialsProvider)
                .endpointOverride(URI.create(endpoint))
                .overrideConfiguration(o -> o.retryStrategy(RetryMode.STANDARD))
                .build();
    }

    public static boolean telemetryEnabled() {
        return Activator.getDefault().getPreferenceStore().getBoolean(AmazonQPreferencePage.TELEMETRY_OPT_IN);
    }

    private void addMetadata(final String key, final Object value, final List<MetadataEntry> entries) {
        if (key != null && value != null) {
            entries.add(MetadataEntry.builder()
                    .key(key)
                    .value(String.valueOf(value))
                    .build());
        }
    }

    public static class Builder {
        private static final Region DEFAULT_TELEMETRY_REGION = Region.US_EAST_1;
        private static final String DEFAULT_TELEMETRY_ENDPOINT = "https://client-telemetry.us-east-1.amazonaws.com";
        private static final String DEFAULT_TELEMETRY_IDENTITY_POOL = "us-east-1:820fd6d1-95c0-4ca4-bffb-3f01d32da842";

        private Region region;
        private String endpoint;
        private String identityPool;
        private ToolkitTelemetryClient telemetryClient;
        private ClientMetadata clientMetadata;

        public final Builder withTelemetryClient(final ToolkitTelemetryClient telemetryClient) {
            this.telemetryClient = telemetryClient;
            return this;
        }

        public final Builder withClientMetadata(final ClientMetadata clientMetadata) {
            this.clientMetadata = clientMetadata;
            return this;
        }

        public final DefaultTelemetryService build() {
            if (telemetryClient == null) {
                telemetryClient = createDefaultTelemetryClient(region != null ? region : DEFAULT_TELEMETRY_REGION,
                        endpoint != null ? endpoint : DEFAULT_TELEMETRY_ENDPOINT,
                        identityPool != null ? identityPool : DEFAULT_TELEMETRY_IDENTITY_POOL);
            }
            if (clientMetadata == null) {
                clientMetadata = PluginClientMetadata.getInstance();
            }
            return new DefaultTelemetryService(this);
        }
    }

}
