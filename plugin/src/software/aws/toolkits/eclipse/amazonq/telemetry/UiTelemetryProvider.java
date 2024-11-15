// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.telemetry;

import software.amazon.awssdk.services.toolkittelemetry.model.MetricDatum;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.telemetry.TelemetryDefinitions.Result;
import software.aws.toolkits.telemetry.UiTelemetry;
import java.time.Instant;

public final class UiTelemetryProvider {

    private UiTelemetryProvider() {
        //prevent instantiation
    }

    public static void emitClickEventMetric(final String elementId) {
        MetricDatum metadata = UiTelemetry.ClickEvent()
            .elementId(elementId)
            .passive(false)
            .createTime(Instant.now())
            .result(Result.SUCCEEDED)
            .build();
        Activator.getTelemetryService().emitMetric(metadata);
    }

}
