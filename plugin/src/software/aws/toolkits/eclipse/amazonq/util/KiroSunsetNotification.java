// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

public final class KiroSunsetNotification extends ToolkitNotification {

    private final Runnable onDismiss;

    public KiroSunsetNotification(final Display display, final String title,
            final String description, final Runnable onDismiss) {
        super(display, title, description);
        this.onDismiss = onDismiss;
    }

    @Override
    protected void createContentArea(final Composite parent) {
        super.createContentArea(parent);

        Composite buttonRow = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        buttonRow.setLayout(layout);
        buttonRow.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));

        Button learnMoreButton = new Button(buttonRow, SWT.PUSH);
        learnMoreButton.setText("Learn more");
        learnMoreButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
        learnMoreButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                PluginUtils.openWebpage(Constants.KIRO_SUNSET_LEARN_MORE_URL);
                dismiss();
            }
        });

        Button dismissButton = new Button(buttonRow, SWT.PUSH);
        dismissButton.setText("Dismiss");
        dismissButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
        dismissButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                dismiss();
            }
        });
    }

    private void dismiss() {
        if (onDismiss != null) {
            onDismiss.run();
        }
        close();
    }
}
