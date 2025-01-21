// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.actions;

import java.util.Objects;

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
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;
import software.aws.toolkits.eclipse.amazonq.observers.EventObserver;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.telemetry.UiTelemetryProvider;
import software.aws.toolkits.eclipse.amazonq.util.Constants;
import software.aws.toolkits.eclipse.amazonq.views.CustomizationDialog;
import software.aws.toolkits.eclipse.amazonq.views.CustomizationDialog.ResponseSelection;
import software.aws.toolkits.eclipse.amazonq.views.model.Customization;

public final class CustomizationDialogContributionItem extends ContributionItem implements EventObserver<AuthState> {
    private static final String CUSTOMIZATION_MENU_ITEM_TEXT = "Select Customization";

    @Inject
    private Shell shell;
    private IViewSite viewSite;

    public CustomizationDialogContributionItem(final IViewSite viewSite) {
        this.viewSite = viewSite;
    }

    // TODO: Need to update this method as the login condition has to be Pro login using IAM identity center
    public void updateVisibility(final AuthState authState) {
        this.setVisible(authState.isLoggedIn() && authState.loginType().equals(LoginType.IAM_IDENTITY_CENTER));
        Display.getDefault().asyncExec(() -> {
            viewSite.getActionBars().getMenuManager().markDirty();
            viewSite.getActionBars().getMenuManager().update(true);
        });
    }

    @Override
    public void onEvent(final AuthState authState) {
        updateVisibility(authState);
    }

    @Override
    public void fill(final Menu menu, final int index) {
        MenuItem menuItem = new MenuItem(menu, SWT.NONE, index);
        menuItem.setText(CUSTOMIZATION_MENU_ITEM_TEXT);
        menuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                UiTelemetryProvider.emitClickEventMetric("ellipses_selectCustomization");
                CustomizationDialog dialog = new CustomizationDialog(shell);
                Customization storedCustomization = Activator.getPluginStore().getObject(
                        Constants.CUSTOMIZATION_STORAGE_INTERNAL_KEY,
                        Customization.class);
                if (Objects.isNull(storedCustomization)) {
                    dialog.setResponseSelection(ResponseSelection.AMAZON_Q_FOUNDATION_DEFAULT);
                    dialog.setSelectedCustomization(null);
                } else {
                    dialog.setResponseSelection(ResponseSelection.CUSTOMIZATION);
                    dialog.setSelectedCustomization(storedCustomization);
                }
                dialog.open();
            }
        });
    }
}
