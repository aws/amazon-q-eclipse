// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;
import software.aws.toolkits.eclipse.amazonq.util.ThreadingUtils;
import software.aws.toolkits.eclipse.amazonq.views.actions.AmazonQStaticActions;

public abstract class BaseView extends ViewPart {
    private String iconPath;
    private String headerLabel;
    private String detailMessage;
    private Composite parentComposite;
    private Composite contentComposite;

    protected abstract String getIconPath();
    protected abstract String getHeaderLabel();
    protected abstract String getDetailMessage();
    protected abstract CompletableFuture<Boolean> isViewDisplayable();
    protected abstract void showAlternateView();

    @Override
    public final void createPartControl(final Composite parent) {
        this.parentComposite = parent;
        this.iconPath = getIconPath();
        this.headerLabel = getHeaderLabel();
        this.detailMessage = getDetailMessage();
        setupView();
        setupStaticMenuActions();

        isViewDisplayable().thenAcceptAsync((isDisplayable) -> {
            if (!isDisplayable) {
                showAlternateView();
            }
        }, ThreadingUtils::executeAsyncTask);
    }

    public final Composite getParentComposite() {
        return parentComposite;
    }

    public final Composite getContentComposite() {
        return contentComposite;
    }

    @Override
    public final void setFocus() {
        parentComposite.setFocus();
    }

    private void setupStaticMenuActions() {
        new AmazonQStaticActions(getViewSite());
    }

    /**
     * Sets up the view components. This method is called during view initialization.
     * Subclasses should override this method to set up their specific view components.
     * <p>
     * When overriding, always call super.setupView() first to ensure proper base initialization.
     */
    protected void setupView() {
        // set margins for parent container
        var layout = new GridLayout(1, false);
        layout.marginLeft = 20;
        layout.marginRight = 20;
        layout.marginTop = 10;
        layout.marginBottom = 10;
        parentComposite.setLayout(layout);

        // set up page layout
        this.contentComposite = new Composite(parentComposite, SWT.NONE);
        contentComposite.setLayout(new GridLayout(1, false));

        // center the content container
        var contentGridData = new GridData(SWT.CENTER, SWT.CENTER, true, true);
        contentComposite.setLayoutData(contentGridData);

        setupIcon(contentComposite);
        setupHeader(contentComposite);
        setupDetailMessage(contentComposite);

        parentComposite.layout(true, true);
    }

    private void setupIcon(final Composite composite) {
        var iconLabel = new Label(composite, SWT.NONE);
        Image icon = loadImage(iconPath);
        iconLabel.setImage(icon);
        iconLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));

        iconLabel.addDisposeListener(e -> {
            if (icon != null && !icon.isDisposed()) {
                icon.dispose();
            }
        });
    }

    private void setupHeader(final Composite composite) {
        var header = new Label(composite,  SWT.CENTER | SWT.WRAP);
        header.setText(headerLabel);
        header.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        var font = magnifyFontSize(parentComposite.getFont(), 18);
        header.setFont(font);

        header.addDisposeListener(e -> {
            if (font != null && !font.isDisposed()) {
                font.dispose();
            }
        });
    }

    private void setupDetailMessage(final Composite composite) {
        var textLabel = new Label(composite,  SWT.CENTER | SWT.WRAP);
        textLabel.setText(detailMessage);
        textLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
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

    private Font magnifyFontSize(final Font originalFont, final int fontSize) {
        FontData[] fontData = originalFont.getFontData();
        for (int i = 0; i < fontData.length; i++) {
            fontData[i].setHeight(fontSize);
        }
        Font magnifiedFont = new Font(parentComposite.getDisplay(), fontData);
        return magnifiedFont;
    }
}
