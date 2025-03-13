// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public final class LspStartUpFailedView extends BaseAmazonQView {
    public static final String ID = "software.aws.toolkits.eclipse.amazonq.views.LspStartUpFailedView";

    private static final String ICON_PATH = "icons/AmazonQ64.png";
    private static final String HEADER_LABEL = "Language Server failed to start.";

    // add logic to base detail_message on error code returned from exception
    private static final String DETAIL_MESSAGE = "Restart Eclipse or review error logs for troubleshooting";
    private Image icon;
    private Composite container;

    @Override
    public Composite setupView(final Composite parentComposite) {
        container = new Composite(parentComposite, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 20;
        layout.marginHeight = 10;
        container.setLayout(layout);

        Label iconLabel = new Label(container, SWT.NONE);
        icon = loadImage(ICON_PATH);
        if (icon != null) {
            iconLabel.setImage(icon);
            iconLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));

            iconLabel.addDisposeListener(e -> {
                if (icon != null && !icon.isDisposed()) {
                    icon.dispose();
                }
            });
        }

        Label headerLabel = new Label(container, SWT.CENTER | SWT.WRAP);
        headerLabel.setText(HEADER_LABEL);
        headerLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        var font = magnifyFontSize(parentComposite, parentComposite.getFont(), 18);
        headerLabel.setFont(font);

        headerLabel.addDisposeListener(e -> {
            if (font != null && !font.isDisposed()) {
                font.dispose();
            }
        });

        Label detailLabel = new Label(container, SWT.CENTER | SWT.WRAP);
        detailLabel.setText(DETAIL_MESSAGE);
        detailLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        setupAmazonQStaticActions();

        return container;
    }

    @Override
    public void dispose() {
        super.dispose();
    }

}
