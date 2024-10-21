// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import java.io.IOException;
import java.net.URL;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.part.ViewPart;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.PluginPlatform;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;

public class DependencyMissingView extends ViewPart {
    public static final String ID = "software.aws.toolkits.eclipse.amazonq.views.DependencyMissingView";
    private static final String EDGE_INSTALL = "https://go.microsoft.com/fwlink/p/?LinkId=2124703";
    private static final String WEBKIT_INSTALL = "https://webkitgtk.org/";
    private static final String EDGE_LEARN_MORE =
            "https://git.eclipse.org/r/plugins/gitiles/platform/eclipse.platform.swt/+/refs/heads/master/bundles/org.eclipse.swt/Readme.WebView2.md";
    private static final String WEBKIT_LEARN_MORE =
            "https://git.eclipse.org/r/plugins/gitiles/platform/eclipse.platform.swt/+/refs/heads/master/bundles/org.eclipse.swt/Readme.Linux.md";

    private Composite parentComposite;
    private PluginPlatform platform;

    @Override
    public final void createPartControl(final Composite parent) {
        platform = PluginUtils.getPlatform();
        this.parentComposite = parent;
        var layout = new GridLayout(1, false);
        parent.setLayout(layout);

        var contentComposite = new Composite(parent, SWT.NONE);
        contentComposite.setLayout(new GridLayout(1, false));

        // center the content holder
        var contentGridData = new GridData(SWT.CENTER, SWT.CENTER, true, true);
        contentComposite.setLayoutData(contentGridData);

        // set up q icon
        var iconLabel = new Label(contentComposite, SWT.NONE);
        Image originalIcon = loadImage("icons/AmazonQ.png");
        Image resizedIcon = resizeImage(originalIcon, 64, 64);
        iconLabel.setImage(resizedIcon);
        iconLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));

        iconLabel.addDisposeListener(e -> {
            if (resizedIcon != null && !resizedIcon.isDisposed()) {
                resizedIcon.dispose();
            }
        });

        // set up the header
        var header = new Label(contentComposite,  SWT.CENTER | SWT.WRAP);
        header.setText("Amazon Q requires " + getDependency() + ", install and restart Eclipse to proceed");
        header.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        header.setFont(magnifyFontSize(parent.getFont(), 18));

        // setup additional text
        var textLabel = new Label(contentComposite,  SWT.CENTER | SWT.WRAP);
        textLabel.setText("Installing this package will provide access to "
        + getDependency() + " within the Amazon Q extension.");
        textLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // setup button to open link to install from
        var installButton = new Button(contentComposite, SWT.PUSH);
        installButton.setText("Install");
        installButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
        installButton.setData("url", getInstallUrl());

        installButton.addSelectionListener(openSelectionInWeb());

        // setup link explaining why this is required
        Link hyperlink = new Link(contentComposite, SWT.NONE);
        hyperlink.setText("<a>Why is this required?</a>");
        hyperlink.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
        hyperlink.setData("url", getLearnMoreUrl());
        hyperlink.addSelectionListener(openSelectionInWeb());

        parent.layout(true, true);
    }

    private String getInstallUrl() {
        return platform == PluginPlatform.WINDOWS ? EDGE_INSTALL : WEBKIT_INSTALL;

    }

    private String getLearnMoreUrl() {
        return platform == PluginPlatform.WINDOWS ? EDGE_LEARN_MORE : WEBKIT_LEARN_MORE;
    }

    private SelectionAdapter openSelectionInWeb() {
        return new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                try {
                    // Open the URL in the external browser
                    String url = (String) e.widget.getData("url");
                    if (url != null) {
                        PluginUtils.openWebpage(url);
                    }
                } catch (Exception ex) {
                    Activator.getLogger().error("Error occured when attempting open url", ex);
                }
            }
        };
    }

    private Image resizeImage(final Image image, final int width, final int height) {
        var originalData = image.getImageData();
        var scaledData = originalData.scaledTo(width, height);
        var scaledImage = new Image(Display.getCurrent(), scaledData);
        image.dispose();
        return scaledImage;
    }

    private Font magnifyFontSize(final Font originalFont, final int fontSize) {
        FontData[] fontData = originalFont.getFontData();
        for (int i = 0; i < fontData.length; i++) {
            fontData[i].setHeight(fontSize);
        }
        Font magnifiedFont = new Font(parentComposite.getDisplay(), fontData);
        return magnifiedFont;
    }

    private String getDependency() {
        return PluginUtils.getPlatform() == PluginPlatform.WINDOWS ? "WebView2" : "WebKit";
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

    @Override
    public final void setFocus() {
        parentComposite.setFocus();
    }
}
