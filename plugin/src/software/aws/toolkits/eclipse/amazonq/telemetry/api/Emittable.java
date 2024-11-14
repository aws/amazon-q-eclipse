// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.telemetry.api;

import software.amazon.awssdk.services.toolkittelemetry.model.MetricDatum;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.telemetry.models.EmittableEventType;
import software.aws.toolkits.eclipse.amazonq.telemetry.models.TelemetryEventRecord;

public abstract class Emittable<T extends TelemetryEventRecord> {

    public final void emit(final EmittableEventType type, final T event) {
        emitDatum(getMetricDatum(type, event));
    }

    protected final void emitDatum(final MetricDatum metricDatum) {
        Activator.getTelemetryService().emitMetric(metricDatum);
    }

    protected abstract MetricDatum getMetricDatum(EmittableEventType type, T event);

}
