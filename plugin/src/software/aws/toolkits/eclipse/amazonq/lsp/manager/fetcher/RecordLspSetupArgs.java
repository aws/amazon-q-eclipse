// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.manager.fetcher;

import software.aws.toolkits.telemetry.TelemetryDefinitions.LanguageServerLocation;

public final class RecordLspSetupArgs {
    private LanguageServerLocation location;
    private String languageServerVersion;
    private String manifestSchemaVersion;
    private double duration;
    private String reason;

    public String getLanguageServerVersion() {
        return languageServerVersion;
    }

    public void setLanguageServerVersion(final String languageServerVersion) {
        this.languageServerVersion = languageServerVersion;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(final double duration) {
        this.duration = duration;
    }

    public String getManifestSchemaVersion() {
        return manifestSchemaVersion;
    }

    public void setManifestSchemaVersion(final String manifestSchemaVersion) {
        this.manifestSchemaVersion = manifestSchemaVersion;
    }

    public LanguageServerLocation getLocation() {
        return location;
    }

    public void setLocation(final LanguageServerLocation location) {
        this.location = location;
    }
    public String getReason() {
        return reason;
    }
    public void setReason(final String reason) {
        this.reason = reason;
    }
}
