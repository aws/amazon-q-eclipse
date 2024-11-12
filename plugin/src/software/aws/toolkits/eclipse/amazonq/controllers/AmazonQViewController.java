// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.controllers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.PluginPlatform;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;

public class AmazonQViewController {
    private boolean hasWebViewDependency = false;
    private PluginPlatform pluginPlatform;
    private Browser browser;

    public AmazonQViewController() {
        this(PluginUtils.getPlatform());
    }
    // Test constructor that accepts a platform
    public AmazonQViewController(final PluginPlatform platform) {
      this.pluginPlatform = platform;
    }

    /*
     * Determines whether the browser type supports rendering webviews for the
     * current platform Note: For Windows, edge is expected; its absence indicates
     * missing WebView2 runtime installation For Linux, WebKit is expected; its
     * absence indicates missing WebKit installation
     *
     * @param browserType The SWT Browser instance's type to check
     *
     * @return true if the browser is compatible, false otherwise
     */
    public final boolean checkWebViewCompatibility(final String browserType) {
        String expectedType = pluginPlatform == PluginPlatform.WINDOWS ? "edge" : "webkit";
        this.hasWebViewDependency = expectedType.equalsIgnoreCase(browserType);
        if (!this.hasWebViewDependency) {
            Activator.getLogger()
                    .info("Browser detected:" + browserType + " is not of expected type: " + expectedType);
        }
        return this.hasWebViewDependency;
    }

    public final int getBrowserStyle() {
        return pluginPlatform == PluginPlatform.WINDOWS ? SWT.EDGE : SWT.WEBKIT;
    }

    /*
     * Sets up the browser compatible with the platform
     * returns boolean representing whether a browser type compatible with webview rendering for the current platform is found
     * @param parent
     */
    public final boolean setupBrowser(final Composite parent) {
        var browser = new Browser(parent, getBrowserStyle());
        checkWebViewCompatibility(browser.getBrowserType());
        // only set the browser if compatible webview browser can be found for the
        // platform
        if (hasWebViewDependency()) {
            this.browser = browser;
        }
        return hasWebViewDependency();
    }

    public final Browser getBrowser() {
        return this.browser;
    }

    /*
     * Gets the status of WebView dependency compatibility
     *
     * @return true if the last check found a compatible WebView, false otherwise
     */
    public final boolean hasWebViewDependency() {
        return this.hasWebViewDependency;
    }

    public final void updateBrowser(final Browser browser) {
        this.browser = browser;
    }
}
