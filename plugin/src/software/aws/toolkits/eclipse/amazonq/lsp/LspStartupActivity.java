// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp;

import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import software.aws.toolkits.eclipse.amazonq.configuration.PluginStore;
import software.aws.toolkits.eclipse.amazonq.util.AutoTriggerDocumentListener;
import software.aws.toolkits.eclipse.amazonq.util.AutoTriggerPartListener;
import software.aws.toolkits.eclipse.amazonq.util.AutoTriggerTopLevelListener;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;
import software.aws.toolkits.eclipse.amazonq.views.ViewConstants;
import software.aws.toolkits.eclipse.amazonq.views.actions.ToggleAutoTriggerContributionItem;

import org.eclipse.lsp4e.LanguageServiceAccessor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.lsp4e.LanguageServersRegistry;

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

                    var authServerDefinition = lsRegistry.getDefinition("software.aws.toolkits.eclipse.amazonq.authServer");
                    LanguageServiceAccessor.startLanguageServer(authServerDefinition);

                    attachAutoTriggerListenersIfApplicable();
                } catch (Exception e) {
                    return new Status(IStatus.ERROR, "amazonq", "Failed to start language server", e);
                }
                return Status.OK_STATUS;
            }
        };
        job.schedule();
        if (PluginStore.get(ViewConstants.PREFERENCE_STORE_PLUGIN_FIRST_STARTUP_KEY) == null) {
            this.launchWebview();
        }
    }

    private void launchWebview() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        workbench.getDisplay().asyncExec(new Runnable() {
            public void run() {
                try {
                    IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
                    if (window != null) {
                        IWorkbenchPage page = window.getActivePage();
                        page.showView("software.aws.toolkits.eclipse.amazonq.views.ToolkitLoginWebview");
                        PluginStore.put(ViewConstants.PREFERENCE_STORE_PLUGIN_FIRST_STARTUP_KEY, "true");
                    }
                } catch (PartInitException e) {
                    PluginLogger.warn("Error occurred during auto loading of plugin", e);
                }
            }
        });
    }

    private void attachAutoTriggerListenersIfApplicable() {
        String autoTriggerPrefValue = PluginStore.get(ToggleAutoTriggerContributionItem.AUTO_TRIGGER_ENABLEMENT_KEY);
        boolean isEnabled = autoTriggerPrefValue != null && !autoTriggerPrefValue.isBlank()
                && autoTriggerPrefValue.equals("true");
        var autoTriggerTopLevelListener = new AutoTriggerTopLevelListener<AutoTriggerPartListener<AutoTriggerDocumentListener>>();
        if (isEnabled) {
            var documentListener = new AutoTriggerDocumentListener();
            var autoTriggerPartListener = new AutoTriggerPartListener<AutoTriggerDocumentListener>(documentListener);
            autoTriggerTopLevelListener.addPartListener(autoTriggerPartListener);
            autoTriggerTopLevelListener.onStart();
        }
        var prefChangeListener = new PreferenceChangeListener() {
            @Override
            public void preferenceChange(PreferenceChangeEvent evt) {
                String keyChanged = evt.getKey();
                String newValue = evt.getNewValue();
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
                    autoTriggerTopLevelListener.onStart();
                } else {
                    // Note to future maintainers: this has to be called from the UI thread or it would not do anything
                    Display.getDefault().asyncExec(() -> {autoTriggerTopLevelListener.onShutdown();});
                }
                System.out.println(keyChanged + " changed to " + newValue);
            }
        };
        PluginStore.addChangeListener(prefChangeListener);
    }

}
