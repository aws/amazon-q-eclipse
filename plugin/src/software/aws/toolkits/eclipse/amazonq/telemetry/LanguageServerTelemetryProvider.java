// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.telemetry;

import software.aws.toolkits.eclipse.amazonq.lsp.manager.fetcher.RecordLspSetupArgs;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.telemetry.LanguageserverTelemetry;
import software.aws.toolkits.telemetry.TelemetryDefinitions.LanguageServerSetupStage;
import software.aws.toolkits.telemetry.TelemetryDefinitions.Result;

public final class LanguageServerTelemetryProvider {
    private LanguageServerTelemetryProvider() {
    }

    public static void emitSetupGetManifest(final Result result, final RecordLspSetupArgs args) {
        emitSetupMetric(result, args, LanguageServerSetupStage.GET_MANIFEST);
    }

    public static void emitSetupGetServer(final Result result, final RecordLspSetupArgs args) {
        emitSetupMetric(result, args, LanguageServerSetupStage.GET_SERVER);
    }

    public static void emitSetupValidate(final Result result, final RecordLspSetupArgs args) {
        emitSetupMetric(result, args, LanguageServerSetupStage.VALIDATE);
    }

    private static void emitSetupMetric(final Result result, final RecordLspSetupArgs args, final LanguageServerSetupStage stage) {
        var data = LanguageserverTelemetry.SetupEvent()
                .id("Amazon Q")
                .passive(true)
                .result(result)
                .reason(args.getReason())
                .duration(args.getDuration())
                .version(args.getLanguageServerVersion())
                .languageServerLocation(args.getLocation())
                .languageServerSetupStage(stage)
                .manifestSchemaVersion(args.getManifestSchemaVersion())
                .build();

        Activator.getTelemetryService().emitMetric(data);
    }
}
