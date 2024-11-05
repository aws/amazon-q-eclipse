// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.telemetry.service;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jface.preference.IPreferenceStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;

import software.amazon.awssdk.services.toolkittelemetry.ToolkitTelemetryClient;
import software.amazon.awssdk.services.toolkittelemetry.model.MetadataEntry;
import software.amazon.awssdk.services.toolkittelemetry.model.MetricDatum;
import software.amazon.awssdk.services.toolkittelemetry.model.PostFeedbackRequest;
import software.amazon.awssdk.services.toolkittelemetry.model.PostMetricsRequest;
import software.amazon.awssdk.services.toolkittelemetry.model.Sentiment;
import software.aws.toolkits.eclipse.amazonq.lsp.model.ErrorData;
import software.aws.toolkits.eclipse.amazonq.extensions.implementation.ActivatorStaticMockExtension;
import software.aws.toolkits.eclipse.amazonq.lsp.model.TelemetryEvent;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.preferences.AmazonQPreferencePage;
import software.aws.toolkits.eclipse.amazonq.telemetry.metadata.ClientMetadata;

public final class DefaultTelemetryServiceTest {

    @RegisterExtension
    private static ActivatorStaticMockExtension activatorStaticMockExtension = new ActivatorStaticMockExtension();

    private DefaultTelemetryService service;
    private ToolkitTelemetryClient mockClient;
    private MockClientMetadata mockClientMetadata;

    @BeforeEach
    public void setUp() {
        mockClient = mock(ToolkitTelemetryClient.class);
        mockClientMetadata = new MockClientMetadata();
        service = new DefaultTelemetryService.Builder()
                .withTelemetryClient(mockClient)
                .withClientMetadata(mockClientMetadata)
                .build();
    }

    @Test
    public void testEmitTelemetryEventTelemetryDisabled() {
        setupMockActivatorWithTelemetryOptIn(false);
        when(mockClient.postMetrics((PostMetricsRequest) any())).thenReturn(null);
        service.emitMetric(new TelemetryEvent("FooEvent", "FooResult", new HashMap<>(), null));
        verify(mockClient, never()).postMetrics(any(PostMetricsRequest.class));
    }

    @Test
    public void testEmitTelemetryEventTelemetryEnabled() {
        setupMockActivatorWithTelemetryOptIn(true);
        when(mockClient.postMetrics(any(PostMetricsRequest.class))).thenReturn(null);
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");
        service.emitMetric(new TelemetryEvent("FooEvent", "FooResult", data, null));
        verify(mockClient).postMetrics(any(PostMetricsRequest.class));
    }

    @Test
    public void testEmitMetricTelemetryDisabled() {
        setupMockActivatorWithTelemetryOptIn(false);
        when(mockClient.postMetrics((PostMetricsRequest) any())).thenReturn(null);
        service.emitMetric(MetricDatum.builder().build());
        verify(mockClient, never()).postMetrics(any(PostMetricsRequest.class));
    }

    @Test
    public void testEmitMetricTelemetryEnabled() {
        setupMockActivatorWithTelemetryOptIn(true);
        when(mockClient.postMetrics(any(PostMetricsRequest.class))).thenReturn(null);
        service.emitMetric(MetricDatum.builder().build());
        verify(mockClient).postMetrics(any(PostMetricsRequest.class));
    }

    @Test
    public void testEmitFeedback() {
        service.emitFeedback("test comment", Sentiment.POSITIVE);
        ArgumentCaptor<PostFeedbackRequest> requestCaptor = ArgumentCaptor.forClass(PostFeedbackRequest.class);
        verify(mockClient).postFeedback(requestCaptor.capture());
        PostFeedbackRequest request = requestCaptor.getValue();

        assertEquals(mockClientMetadata.getPluginName(), request.awsProductAsString());
        assertEquals(mockClientMetadata.getPluginVersion(), request.awsProductVersion());
        assertEquals(mockClientMetadata.getOSName(), request.os());
        assertEquals(mockClientMetadata.getOSVersion(), request.osVersion());
        assertEquals(mockClientMetadata.getIdeName(), request.parentProduct());
        assertEquals(mockClientMetadata.getIdeVersion(), request.parentProductVersion());

        assertEquals("test comment", request.comment());
        assertEquals(Sentiment.POSITIVE, request.sentiment());
    }

    @Test
    public void testEmitMetricDatum() {
        setupMockActivatorWithTelemetryOptIn(true);
        MetricDatum datum = MetricDatum.builder()
                .metricName("test")
                .value(1.0)
                .metadata(MetadataEntry.builder()
                        .key("foo")
                        .value("fooVal")
                        .build())
                .build();
        ArgumentCaptor<PostMetricsRequest> requestCaptor = ArgumentCaptor.forClass(PostMetricsRequest.class);
        service.emitMetric(datum);
        verify(mockClient).postMetrics(requestCaptor.capture());
        PostMetricsRequest request = requestCaptor.getValue();

        assertEquals(mockClientMetadata.getPluginName(), request.awsProductAsString());
        assertEquals(mockClientMetadata.getPluginVersion(), request.awsProductVersion());
        assertEquals(mockClientMetadata.getClientId(), request.clientID());
        assertEquals(mockClientMetadata.getOSName(), request.os());
        assertEquals(mockClientMetadata.getOSVersion(), request.osVersion());
        assertEquals(mockClientMetadata.getIdeName(), request.parentProduct());
        assertEquals(mockClientMetadata.getIdeVersion(), request.parentProductVersion());

        assertEquals(request.metricData().size(), 1);
        MetricDatum requestDatum = request.metricData().get(0);
        assertEquals(datum.metricName(), requestDatum.metricName());
        assertEquals(1.0, requestDatum.value());

        assertEquals(requestDatum.metadata().size(), 1);
        assertTrue(requestDatum.metadata().stream()
                .anyMatch(entry -> entry.key().equals("foo") && entry.value().equals("fooVal")));
    }

    @Test
    public void testEmitMetricTelemetryEvent() {
        setupMockActivatorWithTelemetryOptIn(true);
        Map<String, Object> metadata = Map.of("foo", "fooVal");
        ErrorData errorData = new ErrorData("fooReason", "fooError", 500);
        TelemetryEvent event = new TelemetryEvent("testEvent", "testResult", metadata, errorData);
        ArgumentCaptor<PostMetricsRequest> requestCaptor = ArgumentCaptor.forClass(PostMetricsRequest.class);
        service.emitMetric(event);
        verify(mockClient).postMetrics(requestCaptor.capture());
        PostMetricsRequest request = requestCaptor.getValue();

        assertEquals(mockClientMetadata.getPluginName(), request.awsProductAsString());
        assertEquals(mockClientMetadata.getPluginVersion(), request.awsProductVersion());
        assertEquals(mockClientMetadata.getClientId(), request.clientID());
        assertEquals(mockClientMetadata.getOSName(), request.os());
        assertEquals(mockClientMetadata.getOSVersion(), request.osVersion());
        assertEquals(mockClientMetadata.getIdeName(), request.parentProduct());
        assertEquals(mockClientMetadata.getIdeVersion(), request.parentProductVersion());

        assertEquals(request.metricData().size(), 1);
        MetricDatum requestDatum = request.metricData().get(0);
        assertEquals("testEvent", requestDatum.metricName());
        assertEquals("testEvent", requestDatum.metricName());

        assertEquals(requestDatum.metadata().size(), 5);
        assertTrue(requestDatum.metadata().stream()
                .anyMatch(entry -> entry.key().equals("foo") && entry.value().equals("fooVal")));
        assertTrue(requestDatum.metadata().stream()
                .anyMatch(entry -> entry.key().equals("result") && entry.value().equals("testResult")));
        assertTrue(requestDatum.metadata().stream()
                .anyMatch(entry -> entry.key().equals("reason") && entry.value().equals("fooReason")));
        assertTrue(requestDatum.metadata().stream()
                .anyMatch(entry -> entry.key().equals("errorCode") && entry.value().equals("fooError")));
        assertTrue(requestDatum.metadata().stream()
                .anyMatch(entry -> entry.key().equals("httpStatusCode") && entry.value().equals("500")));
    }

    private void setupMockActivatorWithTelemetryOptIn(final boolean telemetryOptIn) {
        IPreferenceStore mockPreferenceStore = mock(IPreferenceStore.class);
        when(mockPreferenceStore.getBoolean(eq(AmazonQPreferencePage.TELEMETRY_OPT_IN))).thenReturn(telemetryOptIn);
        Optional<Activator> activatorMockOptional = activatorStaticMockExtension.getMock(Activator.class);
        activatorMockOptional.ifPresent(activatorMock ->
                when(activatorMock.getPreferenceStore()).thenReturn(mockPreferenceStore));
    }

    private final class MockClientMetadata implements ClientMetadata {
        @Override
        public String getOSName() {
            return "FooName";
        }

        @Override
        public String getOSVersion() {
            return "FooVersion";
        }

        @Override
        public String getIdeName() {
            return "FooIdeName";
        }

        @Override
        public String getIdeVersion() {
            return "FooIdeVersion";
        }

        @Override
        public String getPluginName() {
            return "FooPluginName";
        }

        @Override
        public String getPluginVersion() {
            return "FooPluginVersion";
        }

        @Override
        public String getClientId() {
            return "FooClientId";
        }
    }
}
