// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import java.util.function.Consumer;
import org.eclipse.swt.SWT;

public final class PersistentToolkitNotification extends ToolkitNotification {
    private final Consumer<Boolean> checkboxCallback;

    public PersistentToolkitNotification(final Display display, final String title,
            final String description, final Consumer<Boolean> checkboxCallback) {
        super(display, title, description);
        this.checkboxCallback = checkboxCallback;
    }

    @Override
    protected void createContentArea(final Composite parent) {
        super.createContentArea(parent);

        Composite container = (Composite) parent.getChildren()[0];

        // create checkbox
        Button doNotShowCheckbox = new Button(container, SWT.CHECK);
        GridData checkboxGridData = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
        doNotShowCheckbox.setLayoutData(checkboxGridData);

        // create checkbox button text
        Label checkboxLabel = new Label(container, SWT.NONE);
        checkboxLabel.setText("Don't show this message again");
        checkboxLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // style button text
        Color grayColor = new Color(parent.getDisplay(), 128, 128, 128);
        checkboxLabel.setForeground(grayColor);

        Font originalFont = checkboxLabel.getFont();
        FontData[] fontData = originalFont.getFontData();
        for (FontData fd : fontData) {
            fd.setHeight(fd.getHeight() - 1);
        }
        Font smallerFont = new Font(parent.getDisplay(), fontData);
        checkboxLabel.setFont(smallerFont);

        checkboxLabel.addDisposeListener(e -> {
            smallerFont.dispose();
            grayColor.dispose();
        });

        // make button text clickable
        checkboxLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseUp(final MouseEvent e) {
                doNotShowCheckbox.setSelection(!doNotShowCheckbox.getSelection());
                if (checkboxCallback != null) {
                    checkboxCallback.accept(doNotShowCheckbox.getSelection());
                }
            }
        });

        doNotShowCheckbox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (checkboxCallback != null) {
                    checkboxCallback.accept(doNotShowCheckbox.getSelection());
                }
            }
        });
    }

}
