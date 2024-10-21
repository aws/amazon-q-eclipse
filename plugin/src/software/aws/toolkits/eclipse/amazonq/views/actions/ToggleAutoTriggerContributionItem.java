// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.views.actions;

import java.io.IOException;
import java.net.URL;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IViewSite;

import software.aws.toolkits.eclipse.amazonq.configuration.PluginStore;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginDetails;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;

public final class ToggleAutoTriggerContributionItem extends ContributionItem {

    public static final String AUTO_TRIGGER_ENABLEMENT_KEY = "aws.q.autotrigger.eclipse";

    private IViewSite viewSite;
    private Image pause;
    private Image resume;

    public ToggleAutoTriggerContributionItem(final IViewSite viewSite) {
        this.viewSite = viewSite;
        pause = loadImage("icons/PauseIcon.png");
        resume = loadImage("icons/AmazonQ.png");
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
        menuItem.setImage(isEnabled ? pause : resume);
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
                menuItem.setImage(wasEnabled ? resume : pause);
            }
        });
    }

    private Image loadImage(final String imagePath) {
        Image loadedImage = null;
        try {
            URL imageUrl = PluginUtils.getResource(imagePath);
            if (imageUrl != null) {
                loadedImage = new Image(Display.getCurrent(), imageUrl.openStream());
            }
        } catch (IOException e) {
            Activator.getLogger().warn(e.getMessage(), e);
        }
        return loadedImage;
    }
}
