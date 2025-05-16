// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import java.util.UUID;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import software.aws.toolkits.eclipse.amazonq.providers.browser.AmazonQBrowserProvider;
import software.aws.toolkits.eclipse.amazonq.util.ThemeDetector;

public abstract class AmazonQView extends BaseAmazonQView {

    private AmazonQBrowserProvider browserProvider;
    private static final ThemeDetector THEME_DETECTOR = new ThemeDetector();
    private final String componentId = UUID.randomUUID().toString();

    protected AmazonQView() {
        this.browserProvider = AmazonQBrowserProvider.getInstance();
    }

    final Browser getBrowser() {
        return browserProvider.getBrowser(componentId);
    }

    public final Browser getAndAttachBrowser(final Composite parent) {
        return browserProvider.getAndAttachBrowser(parent, componentId);
    }

    protected final void setupParentBackground(final Composite parent) {
        Display display = Display.getCurrent();
        Color bg = THEME_DETECTOR.isDarkTheme() ? display.getSystemColor(SWT.COLOR_BLACK)
                : display.getSystemColor(SWT.COLOR_WHITE);
        parent.setBackground(bg);
    }

    protected final Browser setupBrowser(final Composite parent) {
        return browserProvider.setupBrowser(parent, componentId, false);
    }


    protected final void preserveBrowser() {
        browserProvider.preserveBrowser(componentId);
    }

    public final void disposeBrowser() {
        browserProvider.disposeBrowser(componentId);
    }

    /**
     * Sets up the view's browser component and initializes necessary configurations.
     * This method is called during view creation to establish the browser environment.
     *
     * The setup process includes:
     * - Setting up the browser's background color to match the parent
     * - Initializing common actions for the view
     * - Setting up authentication status listeners
     * - Disabling the browser's default context menu
     *
     * @param parent The parent composite where the view will be created
     * @return The configured parent composite containing the view
     */
    @Override
    public Composite setupView(final Composite parent) {
        Browser browser = getBrowser();

        if (browser != null && !browser.isDisposed()) {
            var bgColor = parent.getBackground();
            browser.setBackground(bgColor);
        }

        return parent;
    }

    public final void addFocusListener(final Composite parent, final Browser browser) {
        parent.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(final FocusEvent event) {
                if (!browser.isDisposed()) {
                    browser.setFocus();
                }
            }

            @Override
            public void focusLost(final FocusEvent event) {
                return;
            }
        });
    }

    /**
     * Disposes of the resources associated with this view.
     *
     * This method is called when the view is closed. It removes the authentication
     * status change listener and the selection listener from the page.
     */
    @Override
    public void dispose() {
        super.dispose();
    }

}
