// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.telemetry;

import java.time.Instant;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.telemetry.EclipseTelemetry;
import software.aws.toolkits.telemetry.TelemetryDefinitions.Result;

public final class EclipseTelemetryProvider {

    private EclipseTelemetryProvider() {
        //prevent instantiation
    }

    public static void emitExecuteCommandMetric(final Params params) {
        var metadata = EclipseTelemetry.ExecuteCommandEvent()
                .command(params.command())
                .duration(params.duration())
                .result(params.result())
                .reason(params.reason())
                .passive(false)
                .createTime(Instant.now())
                .value(1.0)
                .build();
        Activator.getTelemetryService().emitMetric(metadata);
    }

    public record Params(String command, double duration, Result result, String reason) { };

}
