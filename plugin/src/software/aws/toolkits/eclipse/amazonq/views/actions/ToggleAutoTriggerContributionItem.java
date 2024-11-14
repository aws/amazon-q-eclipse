// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.views.actions;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IViewSite;

import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.telemetry.UiTelemetryProvider;

public final class ToggleAutoTriggerContributionItem extends ContributionItem {

    public static final String AUTO_TRIGGER_ENABLEMENT_KEY = "aws.q.autotrigger.eclipse";
    private static final String PAUSE_TEXT = "Pause Auto-Suggestions";
    private static final String RESUME_TEXT = "Resume Auto-Suggestions";

    private IViewSite viewSite;
    private Image pause;
    private Image resume;

    public ToggleAutoTriggerContributionItem(final IViewSite viewSite) {
        this.viewSite = viewSite;
        var pauseImageDescriptor = Activator.imageDescriptorFromPlugin("org.eclipse.ui.navigator",
                "icons/full/clcl16/pause.png");
        pause = pauseImageDescriptor.createImage(Display.getCurrent());
        var resumeImageDescriptor = Activator.imageDescriptorFromPlugin("org.eclipse.ui.cheatsheets",
                "icons/elcl16/start_task.png");
        resume = resumeImageDescriptor.createImage(Display.getCurrent());
    }

    public void updateVisibility(final AuthState authState) {
        this.setVisible(authState.isLoggedIn());
        Display.getDefault().asyncExec(() -> {
            viewSite.getActionBars().getMenuManager().markDirty();
            viewSite.getActionBars().getMenuManager().update(true);
        });
    }

    @Override
    public void fill(final Menu menu, final int index) {
        String settingValue = Activator.getPluginStore().get(AUTO_TRIGGER_ENABLEMENT_KEY);
        boolean isEnabled;
        if (settingValue == null) {
            // on by default
            Activator.getPluginStore().put(AUTO_TRIGGER_ENABLEMENT_KEY, "true");
            isEnabled = true;
        } else {
            isEnabled = !settingValue.isBlank() && settingValue.equals("true");
        }
        MenuItem menuItem = new MenuItem(menu, SWT.NONE, index);
        menuItem.setText(isEnabled ? PAUSE_TEXT : RESUME_TEXT);
        menuItem.setImage(isEnabled ? pause : resume);
        menuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                String settingValue = Activator.getPluginStore().get(AUTO_TRIGGER_ENABLEMENT_KEY);
                boolean wasEnabled = settingValue != null && !settingValue.isBlank() && settingValue.equals("true");
                UiTelemetryProvider.emitClickEventMetric((wasEnabled) ? "amazonq_PauseAutoTrigger" : "amazonq_ResumeAutoTrigger");
                if (wasEnabled) {
                    Activator.getPluginStore().put(AUTO_TRIGGER_ENABLEMENT_KEY, "false");
                } else {
                    Activator.getPluginStore().put(AUTO_TRIGGER_ENABLEMENT_KEY, "true");
                }
                menuItem.setText(wasEnabled ? RESUME_TEXT : PAUSE_TEXT);
                menuItem.setImage(wasEnabled ? resume : pause);
            }
        });
    }

    @Override
    public void dispose() {
        pause.dispose();
        resume.dispose();
        super.dispose();
    }
}
