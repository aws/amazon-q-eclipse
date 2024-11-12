// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.telemetry.service;

import software.amazon.awssdk.services.toolkittelemetry.model.MetricDatum;
import software.amazon.awssdk.services.toolkittelemetry.model.Sentiment;
import software.aws.toolkits.eclipse.amazonq.lsp.model.TelemetryEvent;

public interface TelemetryService {

    void emitMetric(TelemetryEvent event);
    void emitMetric(MetricDatum datum);
    void emitFeedback(String comment, Sentiment sentiment);

}
