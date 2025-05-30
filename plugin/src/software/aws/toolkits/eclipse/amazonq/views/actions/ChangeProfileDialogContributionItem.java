// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.actions;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import jakarta.inject.Inject;
import software.aws.toolkits.eclipse.amazonq.views.ChangeProfileDialog;

public final class ChangeProfileDialogContributionItem extends ContributionItem {
    private static final String CHANGE_PROFILE_MENU_TEXT = "Change Profile";

    @Inject
    private Shell shell;

    @Override
    public void setVisible(final boolean isVisible) {
        super.setVisible(isVisible);
    }

    @Override
    public void fill(final Menu menu, final int index) {
        MenuItem menuItem = new MenuItem(menu, SWT.NONE, index);
        menuItem.setText(CHANGE_PROFILE_MENU_TEXT);
        menuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                ChangeProfileDialog dialog = new ChangeProfileDialog(shell);
                dialog.open();
            }
        });
    }

}

