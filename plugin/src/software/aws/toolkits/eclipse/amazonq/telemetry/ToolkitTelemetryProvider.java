// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.telemetry;

import software.amazon.awssdk.services.toolkittelemetry.model.MetricDatum;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.telemetry.TelemetryDefinitions.Result;
import software.aws.toolkits.telemetry.ToolkitTelemetry;
import java.time.Instant;

public final class ToolkitTelemetryProvider {

    private ToolkitTelemetryProvider() {
        //prevent instantiation
    }

    public static void emitOpenModuleEventMetric(final String module, final String source, final String failureReason) {
        Result result = Result.SUCCEEDED;
        boolean isPassive = (!source.equals("ellipsesMenu") && !source.equals("statusBar") && !source.equals("shortcut"));

        if (!failureReason.equals("none")) {
            result = Result.FAILED;
            ToolkitTelemetry.OpenModuleEvent().reason(failureReason);
        }
        MetricDatum metadata = ToolkitTelemetry.OpenModuleEvent()
            .module(mapModuleId(module))
            .result(result)
            .source(source)
            .passive(isPassive)
            .createTime(Instant.now())
            .value(1.0)
            .build();
        Activator.getTelemetryService().emitMetric(metadata);
    }
    public static void emitCloseModuleEventMetric(final String module, final String failureReason) {
        Result result = (failureReason.equals("none")) ? Result.SUCCEEDED : Result.FAILED;
        MetricDatum metadata = ToolkitTelemetry.CloseModuleEvent()
                .module(mapModuleId(module))
                .result(result)
                .passive(true)
                .createTime(Instant.now())
                .value(1.0)
                .build();
        Activator.getTelemetryService().emitMetric(metadata);
    }
    private static String mapModuleId(final String viewId) {
        String page = viewId.substring(viewId.lastIndexOf(".") + 1);
        switch (page) {
        case "ToolkitLoginWebview":
            return "AuthView";
        case "AmazonQChatWebview":
            return "ChatView";
        case "AmazonQCodeReferenceView":
            return "CodeReferenceView";
        default:
            return page;
        }
    }

}
