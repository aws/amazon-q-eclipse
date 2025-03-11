// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.lsp4e.LanguageServersRegistry;
import org.eclipse.lsp4e.LanguageServiceAccessor;
import org.eclipse.mylyn.commons.ui.dialogs.AbstractNotificationPopup;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.providers.browser.AmazonQBrowserProvider;
import software.aws.toolkits.eclipse.amazonq.telemetry.ToolkitTelemetryProvider;
import software.aws.toolkits.eclipse.amazonq.telemetry.metadata.ExceptionMetadata;
import software.aws.toolkits.eclipse.amazonq.util.AutoTriggerDocumentListener;
import software.aws.toolkits.eclipse.amazonq.util.AutoTriggerPartListener;
import software.aws.toolkits.eclipse.amazonq.util.AutoTriggerTopLevelListener;
import software.aws.toolkits.eclipse.amazonq.util.Constants;
import software.aws.toolkits.eclipse.amazonq.util.ToolkitNotification;
import software.aws.toolkits.eclipse.amazonq.util.UpdateUtils;
import software.aws.toolkits.eclipse.amazonq.views.ViewConstants;
import software.aws.toolkits.eclipse.amazonq.views.ViewVisibilityManager;
import software.aws.toolkits.eclipse.amazonq.views.actions.ToggleAutoTriggerContributionItem;

@SuppressWarnings("restriction")
public class LspStartupActivity implements IStartup {

    @Override
    public final void earlyStartup() {
        Job job = new Job("Start language servers") {
            @Override
            protected IStatus run(final IProgressMonitor monitor) {
                try {
                    var lsRegistry = LanguageServersRegistry.getInstance();
                    var qServerDefinition = lsRegistry.getDefinition("software.aws.toolkits.eclipse.amazonq.qlanguageserver");
                    LanguageServiceAccessor.startLanguageServer(qServerDefinition);
                    Class.forName("software.aws.toolkits.eclipse.amazonq.util.AutoTriggerTopLevelListener");
                    Thread.sleep(2000);
                    Display.getDefault().asyncExec(() -> attachAutoTriggerListenersIfApplicable());
                } catch (Exception e) {
                    return new Status(IStatus.ERROR, "amazonq", "Failed to start language server", e);
                }
                return Status.OK_STATUS;
            }
        };
        job.setSystem(true); // prevent user notification
        job.schedule();
        if (Activator.getPluginStore().get(ViewConstants.PREFERENCE_STORE_PLUGIN_FIRST_STARTUP_KEY) == null) {
            Activator.getLspProvider().getAmazonQServer();
            this.launchWebview();
        }
        Job updateCheckJob = new Job("Check for updates") {
            @Override
            protected IStatus run(final IProgressMonitor monitor) {
                try {
                    Thread.sleep(1000);
                    Class.forName("software.aws.toolkits.eclipse.amazonq.util.UpdateUtils");
                    Display.getDefault().asyncExec(() -> UpdateUtils.getInstance().checkForUpdate());
                } catch (Exception e) {
                    return new Status(IStatus.WARNING, "amazonq", "Failed to check for updates", e);
                }
                return Status.OK_STATUS;
            }
        };

        updateCheckJob.setPriority(Job.DECORATE);
        updateCheckJob.setSystem(true);
        updateCheckJob.schedule();

        new AmazonQBrowserProvider().publishBrowserCompatibilityState();
    }

    private void launchWebview() {
        String viewId = "software.aws.toolkits.eclipse.amazonq.views.AmazonQViewContainer";

        IWorkbench workbench = PlatformUI.getWorkbench();
        workbench.getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    showTelemetryNotification();
                    IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
                    if (window != null) {
                        ToolkitTelemetryProvider.emitOpenModuleEventMetric(viewId, "firstStartUp", "none");
                        Activator.getPluginStore().put(ViewConstants.PREFERENCE_STORE_PLUGIN_FIRST_STARTUP_KEY, "true");
                    }
                    ViewVisibilityManager.showDefaultView("launch");
                } catch (Exception e) {
                    Activator.getLogger().warn("Error occurred during auto loading of plugin", e);
                    ToolkitTelemetryProvider.emitOpenModuleEventMetric(viewId, "firstStartUp",
                            ExceptionMetadata.scrubException("Plugin load error", e));
                }
            }
        });
    }

    private void attachAutoTriggerListenersIfApplicable() {
        String autoTriggerPrefValue = Activator.getPluginStore().get(ToggleAutoTriggerContributionItem.AUTO_TRIGGER_ENABLEMENT_KEY);
        boolean isEnabled = autoTriggerPrefValue != null && !autoTriggerPrefValue.isBlank()
                && autoTriggerPrefValue.equals("true");
        var autoTriggerTopLevelListener = new AutoTriggerTopLevelListener<AutoTriggerPartListener<AutoTriggerDocumentListener>>();
        if (isEnabled) {
            var documentListener = new AutoTriggerDocumentListener();
            var autoTriggerPartListener = new AutoTriggerPartListener<AutoTriggerDocumentListener>(documentListener);
            autoTriggerTopLevelListener.addPartListener(autoTriggerPartListener);
            autoTriggerTopLevelListener.onStart();
        }
        var prefChangeListener = new IPreferenceChangeListener() {
            @Override
            public void preferenceChange(final PreferenceChangeEvent evt) {
                String keyChanged = evt.getKey();
                String newValue = (String) evt.getNewValue();
                if (!keyChanged.equals(ToggleAutoTriggerContributionItem.AUTO_TRIGGER_ENABLEMENT_KEY)) {
                    return;
                }
                boolean isEnabled = newValue != null && !newValue.isBlank() && newValue.equals("true");
                if (isEnabled) {
                    if (autoTriggerTopLevelListener.getPartListener() == null) {
                        var documentListener = new AutoTriggerDocumentListener();
                        var autoTriggerPartListener = new AutoTriggerPartListener<AutoTriggerDocumentListener>(documentListener);
                        autoTriggerTopLevelListener.addPartListener(autoTriggerPartListener);
                    }
                    Display.getDefault().asyncExec(() -> {
                        autoTriggerTopLevelListener.onStart();
                    });
                } else {
                    // Note to future maintainers: this has to be called from the UI thread or it would not do anything
                    Display.getDefault().asyncExec(() -> {
                        autoTriggerTopLevelListener.onShutdown();
                    });
                }
            }
        };
        Activator.getPluginStore().addChangeListener(prefChangeListener);
    }

    private void showTelemetryNotification() {
        AbstractNotificationPopup notification = new ToolkitNotification(Display.getCurrent(),
                Constants.TELEMETRY_NOTIFICATION_TITLE,
                Constants.TELEMETRY_NOTIFICATION_BODY);
        notification.open();
    }

}
