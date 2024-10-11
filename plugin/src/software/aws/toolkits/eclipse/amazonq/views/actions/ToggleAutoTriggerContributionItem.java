// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.views.actions;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PlatformUI;

import software.aws.toolkits.eclipse.amazonq.configuration.PluginStore;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginDetails;

public final class ToggleAutoTriggerContributionItem extends ContributionItem {

    public static final String AUTO_TRIGGER_ENABLEMENT_KEY = "aws.q.autotrigger.eclipse";

    private IViewSite viewSite;

    public ToggleAutoTriggerContributionItem(final IViewSite viewSite) {
        this.viewSite = viewSite;
    }

    public void updateVisibility(final LoginDetails loginDetails) {
        this.setVisible(loginDetails.getIsLoggedIn());
        Display.getDefault().asyncExec(() -> {
            viewSite.getActionBars().getMenuManager().markDirty();
            viewSite.getActionBars().getMenuManager().update(true);
        });
    }

    @Override
    public void fill(final Menu menu, final int index) {
        String settingValue = PluginStore.get(AUTO_TRIGGER_ENABLEMENT_KEY);
        boolean isEnabled = settingValue != null && !settingValue.isBlank() && settingValue.equals("true");
        MenuItem menuItem = new MenuItem(menu, SWT.NONE, index);
        menuItem.setText(isEnabled ? "Pause auto trigger" : "Resume auto trigger");
        menuItem.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_UNDO_HOVER));
        menuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                String settingValue = PluginStore.get(AUTO_TRIGGER_ENABLEMENT_KEY);
                boolean wasEnabled = settingValue != null && !settingValue.isBlank() && settingValue.equals("true");
                if (wasEnabled) {
                    PluginStore.remove(AUTO_TRIGGER_ENABLEMENT_KEY);
                } else {
                    PluginStore.put(AUTO_TRIGGER_ENABLEMENT_KEY, "true");
                }
                menuItem.setText(wasEnabled ? "Resume auto trigger" : "Pause auto trigger");
            }
        });
    }
}
