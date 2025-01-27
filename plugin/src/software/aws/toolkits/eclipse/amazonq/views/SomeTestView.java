// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class SomeTestView implements BaseAmazonQView {
    private Font titleFont; // Keep reference to dispose later

    @Override
    public Composite setupView(Composite parentComposite) {
        // Create a new composite with a GridLayout
        Composite container = new Composite(parentComposite, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        container.setLayout(layout);

        // Add a label with some text
        Label label = new Label(container, SWT.NONE);
        label.setText("This is Test View A");
        label.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));

        // Optional: Add some styling to make it more visible
        titleFont = new Font(container.getDisplay(), "Arial", 14, SWT.BOLD);
        label.setFont(titleFont);

        return container;
    }

    @Override
    public void dispose() {
        if (titleFont != null && !titleFont.isDisposed()) {
            titleFont.dispose();
            titleFont = null;
        }
    }

    @Override
    public boolean canDisplay() {
        return true;
    }
}
