// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.plugin;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import software.aws.toolkits.eclipse.amazonq.chat.ChatStateManager;
import software.aws.toolkits.eclipse.amazonq.configuration.DefaultPluginStore;
import software.aws.toolkits.eclipse.amazonq.configuration.PluginStore;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.DefaultLoginService;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.LoginService;
import software.aws.toolkits.eclipse.amazonq.providers.LspProvider;
import software.aws.toolkits.eclipse.amazonq.providers.LspProviderImpl;
import software.aws.toolkits.eclipse.amazonq.telemetry.TelemetryEmitterManager;
import software.aws.toolkits.eclipse.amazonq.telemetry.api.Emittable;
import software.aws.toolkits.eclipse.amazonq.telemetry.models.TelemetryEventRecord;
import software.aws.toolkits.eclipse.amazonq.telemetry.service.DefaultTelemetryService;
import software.aws.toolkits.eclipse.amazonq.telemetry.service.TelemetryService;
import software.aws.toolkits.eclipse.amazonq.util.LoggingService;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;

public class Activator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "amazon-q-eclipse";
    private static Activator plugin;
    private static TelemetryService telemetryService;
    private static LoggingService defaultLogger;
    private static LspProvider lspProvider;
    private static LoginService loginService;
    private static PluginStore pluginStore;
    private static TelemetryEmitterManager telemetryEmitterManager;

    public Activator() {
        super();
        plugin = this;
        defaultLogger = PluginLogger.getInstance();
        telemetryService = DefaultTelemetryService.builder().build();
        lspProvider = LspProviderImpl.getInstance();
        pluginStore = DefaultPluginStore.getInstance();
        telemetryEmitterManager = new TelemetryEmitterManager();
        loginService = DefaultLoginService.builder()
                .withLspProvider(lspProvider)
                .withPluginStore(pluginStore)
                .initializeOnStartUp()
                .build();
    }

    @Override
    public final void stop(final BundleContext context) throws Exception {
        ChatStateManager.getInstance().dispose();
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
    public static LspProvider getLspProvider() {
        return lspProvider;
    }
    public static LoginService getLoginService() {
        return loginService;
    }
    public static PluginStore getPluginStore() {
        return pluginStore;
    }
    public static <T extends TelemetryEventRecord> Emittable<T> getTelemetryEmitterFor(final Class<T> type) {
        return telemetryEmitterManager.getEmitterFor(type);
    }

}
