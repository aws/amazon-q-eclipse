// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.plugin;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import software.aws.toolkits.eclipse.amazonq.telemetry.service.DefaultTelemetryService;
import software.aws.toolkits.eclipse.amazonq.telemetry.service.TelemetryService;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;
import software.aws.toolkits.eclipse.amazonq.util.LoggingService;

public class Activator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "amazon-q-eclipse";
    private static Activator plugin;
    private static TelemetryService telemetryService;
    private static LoggingService defaultLogger;

    public Activator() {
        super();
        plugin = this;
        telemetryService = DefaultTelemetryService.builder().build();
        defaultLogger = PluginLogger.getInstance();
    }

    @Override
    public final void stop(final BundleContext context) throws Exception {
        super.stop(context);
        plugin = null;
    }

    public static Activator getDefault() {
        return plugin;
    }

    // TODO: replace with proper injection pattern
    public static TelemetryService getTelemetryService() {
        return telemetryService;
    }
    public static LoggingService getLogger() {
        return defaultLogger;
    }

}
