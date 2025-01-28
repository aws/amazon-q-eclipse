// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import java.io.IOException;
import java.net.URL;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;

public abstract class CallToActionView implements BaseAmazonQView {
    private String buttonLabel;
    private SelectionListener buttonHandler;

    private final String ICON_PATH = getIconPath();
    private final String HEADER_LABEL = getHeaderLabel();
    private final String DETAIL_MESSAGE = getDetailMessage();
    private Image icon;

    protected abstract String getButtonLabel();
    protected abstract SelectionListener getButtonHandler();
    protected abstract void setupButtonFooterContent(Composite composite);

    @Override
     public Composite setupView(Composite parentComposite) {
        Composite container = new Composite(parentComposite, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 10;
        layout.marginHeight = 10;
        container.setLayout(layout);

        // Center the container itself
        container.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));

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

        Label detailLabel = new Label(container, SWT.CENTER | SWT.WRAP);
        detailLabel.setText(DETAIL_MESSAGE);
        detailLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        this.buttonLabel = getButtonLabel();
        this.buttonHandler = getButtonHandler();
        setupButton(container);
        setupButtonFooterContent(container);

        return container;
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
    public void dispose() {
        // Default implementation - subclasses can override if they need to dispose of resources
    }

    @Override
    public boolean canDisplay() {
        // Default implementation - subclasses should override to provide specific display logic
        return true;
    }
    protected String getIconPath() {
        // TODO Auto-generated method stub
        return null;
    }
    protected String getHeaderLabel() {
        // TODO Auto-generated method stub
        return null;
    }
    protected String getDetailMessage() {
        // TODO Auto-generated method stub
        return null;
    }

}
