// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.actions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

import software.aws.toolkits.eclipse.amazonq.util.QInvocationSession;

public final class InlineQueryStatusBarContribution extends WorkbenchWindowControlContribution {

    private static final String QUERY_STATUS   = "Amazon Q is querying...    ";
    private static final String IDLE_STATUS    = "Amazon Q is ready          ";
    private static final String PREVIEW_STATUS = "Amazon Q is previewing     ";

    private Label statusLabel;

    public InlineQueryStatusBarContribution() {
        super("globalStatusBar");
    }

    @Override
    protected Control createControl(final Composite parent) {
        GridData parentData = new GridData(SWT.FILL, SWT.FILL, true, false);
        parentData.verticalIndent = 0;
        parent.setLayoutData(parentData);

        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginLeft = 1;
        layout.marginRight = 1;
        layout.horizontalSpacing = 0;
        layout.verticalSpacing = 0;
        parent.setLayout(layout);

        statusLabel = new Label(parent, SWT.NONE);
        statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
        statusLabel.setText(IDLE_STATUS);
        QInvocationSession session = QInvocationSession.getInstance();
        session.assignQueryingCallback(new Runnable() {
            @Override
            public void run() {
                statusLabel.getDisplay().asyncExec(() -> {
                    statusLabel.setText(QUERY_STATUS);
                    statusLabel.update();
                    statusLabel.redraw();
                });
            }
        });
        session.assignIdlingCallback(new Runnable() {
            @Override
            public void run() {
                statusLabel.getDisplay().asyncExec(() -> {
                    statusLabel.setText(IDLE_STATUS);
                    statusLabel.update();
                    statusLabel.redraw();
                });
            }
        });
        session.assignPreviewingCallback(new Runnable() {
            @Override
            public void run() {
                statusLabel.getDisplay().asyncExec(() -> {
                    statusLabel.setText(PREVIEW_STATUS);
                    statusLabel.update();
                    statusLabel.redraw();
                });
            }
        });
        return statusLabel;
    }
}
