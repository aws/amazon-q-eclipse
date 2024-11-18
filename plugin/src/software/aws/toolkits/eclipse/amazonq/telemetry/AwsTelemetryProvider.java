// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.telemetry;

import java.time.Instant;

import software.amazon.awssdk.services.toolkittelemetry.model.MetricDatum;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.telemetry.AwsTelemetry;

public final class AwsTelemetryProvider {

    private AwsTelemetryProvider() {
        //prevent instantiation
    }

    public static void emitModifySettingEvent(final String settingId, final String settingState) {
        MetricDatum metricDatum = AwsTelemetry.ModifySettingEvent()
                .settingId(settingId)
                .settingState(settingState)
                .passive(false)
                .createTime(Instant.now())
                .value(1.0)
                .build();
        Activator.getTelemetryService().emitMetric(metricDatum);
    }

}
