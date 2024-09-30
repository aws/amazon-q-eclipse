// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.actions;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewSite;
import jakarta.inject.Inject;
import software.amazon.awssdk.utils.StringUtils;
import software.aws.toolkits.eclipse.amazonq.configuration.PluginStore;
import software.aws.toolkits.eclipse.amazonq.util.AuthStatusChangedListener;
import software.aws.toolkits.eclipse.amazonq.views.CustomizationDialog;
import software.aws.toolkits.eclipse.amazonq.views.CustomizationDialog.ResponseSelection;
import software.aws.toolkits.eclipse.amazonq.views.model.Customization;

public final class CustomizationDialogContributionItem extends ContributionItem implements AuthStatusChangedListener {
    private static final String CUSTOMIZATION_MENU_ITEM_TEXT = "Select Customization";

    @Inject
    private Shell shell;
    private IViewSite viewSite;

    public CustomizationDialogContributionItem(final IViewSite viewSite) {
        this.viewSite = viewSite;
    }

    // TODO: Need to update this method as the login condition has to be Pro login using IAM identity center
    public void updateVisibility(final boolean isLoggedIn) {
        this.setVisible(isLoggedIn);
        Display.getDefault().asyncExec(() -> {
            viewSite.getActionBars().getMenuManager().markDirty();
            viewSite.getActionBars().getMenuManager().update(true);
        });
    }

    @Override
    public void onAuthStatusChanged(final boolean isLoggedIn) {
        updateVisibility(isLoggedIn);
    }

    private List<Customization> getCustomizations() {
        List<Customization> customizations = new ArrayList<>();
        customizations.add(new Customization("customization-arn-1", "Customization 1", "Code Whisperer customization 1"));
        customizations.add(new Customization("customization-arn-2", "Customization 2", "Code Whisperer customization 2"));
        customizations.add(new Customization("customization-arn-3", "Customization 3", "Code Whisperer customization 3"));
        return customizations;
    }

    @Override
    public void fill(final Menu menu, final int index) {
        MenuItem menuItem = new MenuItem(menu, SWT.NONE, index);
        menuItem.setText(CUSTOMIZATION_MENU_ITEM_TEXT);
        menuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                CustomizationDialog dialog = new CustomizationDialog(shell);
                // TODO: This mock will be replaced by an actual call to LSP
                dialog.setCustomisationResponse(getCustomizations());
                String storedCustomizationArn = PluginStore.get(CustomizationDialog.CUSTOMIZATION_STORAGE_INTERNAL_KEY);
                if (StringUtils.isBlank(storedCustomizationArn)) {
                    dialog.setResponseSelection(ResponseSelection.AMAZON_Q_FOUNDATION_DEFAULT);
                    dialog.setSelectedCustomizationArn(null);
                } else {
                    dialog.setResponseSelection(ResponseSelection.CUSTOMIZATION);
                    dialog.setSelectedCustomizationArn(storedCustomizationArn);
                }
                dialog.open();
            }
        });
    }
}
