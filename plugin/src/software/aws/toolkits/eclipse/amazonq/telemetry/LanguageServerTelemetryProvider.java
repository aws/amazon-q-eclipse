// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.telemetry;

import java.time.Duration;
import java.time.Instant;

import software.aws.toolkits.eclipse.amazonq.lsp.manager.fetcher.RecordLspSetupArgs;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.telemetry.LanguageserverTelemetry;
import software.aws.toolkits.telemetry.TelemetryDefinitions.LanguageServerLocation;
import software.aws.toolkits.telemetry.TelemetryDefinitions.LanguageServerSetupStage;
import software.aws.toolkits.telemetry.TelemetryDefinitions.ManifestLocation;
import software.aws.toolkits.telemetry.TelemetryDefinitions.Result;

public final class LanguageServerTelemetryProvider {
    private static Instant initStartPoint;
    private static Instant allStartPoint;
    private static Instant manifestStartPoint;

    private LanguageServerTelemetryProvider() {
        //prevent instantiation
    }

    public static void setInitStartPoint(final Instant start) {
        initStartPoint = start;
    }
    public static void setAllStartPoint(final Instant start) {
        allStartPoint = start;
    }
    public static void setManifestStartPoint(final Instant start) {
        manifestStartPoint = start;
    }

    public static void emitSetupGetManifest(final Result result, final RecordLspSetupArgs args) {
        args.setDuration(Duration.between(manifestStartPoint, Instant.now()).toMillis());
        emitSetupMetric(result, args, LanguageServerSetupStage.GET_MANIFEST);
        if (result == Result.FAILED && args.getManifestLocation() == ManifestLocation.UNKNOWN) {
            emitSetupAll(Result.FAILED, args);
        }
    }

    public static void emitSetupGetServer(final Result result, final RecordLspSetupArgs args) {
        emitSetupMetric(result, args, LanguageServerSetupStage.GET_SERVER);
        if (result == Result.FAILED && args.getLocation() == LanguageServerLocation.UNKNOWN) {
            emitSetupAll(Result.FAILED, args);
        }
    }

    public static void emitSetupValidate(final Result result, final RecordLspSetupArgs args) {
        emitSetupMetric(result, args, LanguageServerSetupStage.VALIDATE);
        if (result == Result.FAILED && args.getLocation() != LanguageServerLocation.OVERRIDE) {
            emitSetupAll(Result.FAILED, args);
        }
    }
    public static void emitSetupInitialize(final Result result, final RecordLspSetupArgs args) {
        args.setDuration(Duration.between(initStartPoint, Instant.now()).toMillis());
        emitSetupMetric(result, args, LanguageServerSetupStage.INITIALIZE);

        //final step completing makes call to complete full process
        emitSetupAll(result, args);
    }
    public static void emitSetupAll(final Result result, final RecordLspSetupArgs args) {
        args.setDuration(Duration.between(allStartPoint, Instant.now()).toMillis());
        emitSetupMetric(result, args, LanguageServerSetupStage.ALL);
    }

    /*TODO: pass errorCode() into metric as well
     * To separate reason field from error code
     */
    private static void emitSetupMetric(final Result result, final RecordLspSetupArgs args, final LanguageServerSetupStage stage) {
        var data = LanguageserverTelemetry.SetupEvent()
                .id("Amazon Q")
                .passive(true)
                .result(result)
                .reason(args.getReason())
                .duration(args.getDuration())
                .version(args.getLanguageServerVersion())
                .languageServerLocation(args.getLocation())
                .manifestLocation(args.getManifestLocation())
                .languageServerSetupStage(stage)
                .manifestSchemaVersion(args.getManifestSchemaVersion())
                .createTime(Instant.now())
                .value(1.0)
                .build();

        Activator.getTelemetryService().emitMetric(data);
    }
}
