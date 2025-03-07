// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public abstract class CallToActionView extends BaseAmazonQView {
    private String buttonLabel;
    private SelectionListener buttonHandler;

    private final String iconPath = getIconPath();
    private final String headerLabel = getHeaderLabel();
    private final String detailMessage = getDetailMessage();
    private Image icon;

    protected abstract String getButtonLabel();
    protected abstract SelectionListener getButtonHandler();
    protected abstract void setupButtonFooterContent(Composite composite);

    @Override
    public final Composite setupView(final Composite parentComposite) {
        Composite container = new Composite(parentComposite, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 10;
        layout.marginHeight = 10;
        container.setLayout(layout);

        // Center the container itself
        container.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));

        Label iconLabel = new Label(container, SWT.NONE);
        icon = loadImage(iconPath);
        if (icon != null) {
            iconLabel.setImage(icon);
            iconLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));

            iconLabel.addDisposeListener(e -> {
                if (icon != null && !icon.isDisposed()) {
                    icon.dispose();
                }
            });
        }

        Label header = new Label(container, SWT.CENTER | SWT.WRAP);
        header.setText(headerLabel);
        header.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Label detailLabel = new Label(container, SWT.CENTER | SWT.WRAP);
        detailLabel.setText(detailMessage);
        detailLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        this.buttonLabel = getButtonLabel();
        this.buttonHandler = getButtonHandler();
        setupButton(container);
        setupButtonFooterContent(container);

        setupAmazonQStaticActions();

        return container;
    }

    private void setupButton(final Composite composite) {
        var button = new Button(composite, SWT.PUSH);
        updateButtonStyle(button);
        button.setText(buttonLabel);
        button.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
        button.addSelectionListener(buttonHandler);
    }

    /**
     * Updates the button style as required.
     * @param button the component to apply style update
     *
     * Default protected method that does nothing. This method can be overridden by subclasses to customize button style
     * during view creation.
     */
    protected void updateButtonStyle(final Button button) {
        return;
    }

    @Override
    public final void dispose() {
        super.dispose();
    }

    protected abstract String getIconPath();
    protected abstract String getHeaderLabel();
    protected abstract String getDetailMessage();

}
