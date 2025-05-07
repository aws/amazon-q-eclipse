// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.providers.browser;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import software.aws.toolkits.eclipse.amazonq.broker.events.BrowserCompatibilityState;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.PluginPlatform;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;

public final class AmazonQBrowserProvider {
    private static AmazonQBrowserProvider instance;

    private boolean hasWebViewDependency = false;
    private PluginPlatform pluginPlatform;
    private Map<String, Browser> browserById;
    private Map<String, Composite> compositeById;

    private AmazonQBrowserProvider(final Builder builder) {
        this.pluginPlatform = builder.pluginPlatform;
        browserById = new HashMap<>();
        compositeById = new HashMap<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static synchronized AmazonQBrowserProvider getInstance() {
        if (instance == null) {
            instance = AmazonQBrowserProvider.builder().build();
        }
        return instance;
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
    public synchronized boolean checkWebViewCompatibility(final String browserType,
            final boolean publishUnconditionally) {
        String expectedType = pluginPlatform == PluginPlatform.WINDOWS ? "edge" : "webkit";
        boolean hasWebViewDependency = expectedType.equalsIgnoreCase(browserType);

        if (!hasWebViewDependency) {
            Activator.getLogger()
                    .info("Browser detected:" + browserType + " is not of expected type: " + expectedType);
        }

        if (publishUnconditionally || this.hasWebViewDependency != hasWebViewDependency) {
            Activator.getEventBroker().post(BrowserCompatibilityState.class,
                    hasWebViewDependency ? BrowserCompatibilityState.COMPATIBLE
                            : BrowserCompatibilityState.DEPENDENCY_MISSING);

        }
        this.hasWebViewDependency = hasWebViewDependency;
        return this.hasWebViewDependency;
    }

    public synchronized int getBrowserStyle() {
        return pluginPlatform == PluginPlatform.WINDOWS ? SWT.EDGE : SWT.WEBKIT;
    }

    /*
     * Sets up the browser compatible with the platform
     * returns boolean representing whether a browser type compatible with webview rendering for the current platform is found
     * @param parent
     */
    public synchronized Browser setupBrowser(final Composite parent, final String componentId,
            final boolean publishUnconditionally) {
        var browser = new Browser(parent, getBrowserStyle());

        GridData layoutData = new GridData(GridData.FILL_BOTH);
        browser.setLayoutData(layoutData);

        checkWebViewCompatibility(browser.getBrowserType(), publishUnconditionally);

        // only set the browser if compatible webview browser can be found for the
        // platform
        if (hasWebViewDependency()) {
            browserById.put(componentId, browser);
            return browser;
        }
        return null;
    }

    public synchronized Browser getBrowser(final String componentId) {
        return browserById.get(componentId);
    }

    private synchronized Composite getDummyParent(final String componentId) {
        return compositeById.get(componentId);
    }

    public synchronized Browser getAndAttachBrowser(final Composite parent, final String componentId) {
        var browser = getBrowser(componentId);

        // if browser is null or disposed, return null
        if (browser == null || browser.isDisposed()) {
            return null;
        } else if (browser.getParent() != parent) {
            // Re-parent existing browser
            browser.setParent(parent);
            disposeDummyParent(componentId);
        }
        return browser;
    }

    public synchronized void preserveBrowser(final String componentId) {
        var browser = getBrowser(componentId);
        var dummyParent = getDummyParent(componentId);

        if (browser != null && !browser.isDisposed()) {
            if (dummyParent == null || dummyParent.isDisposed()) {
                dummyParent = new Composite(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.NONE);
                dummyParent.setVisible(false);
            }
            browser.setParent(dummyParent);
            compositeById.put(componentId, dummyParent);
        }
    }

    private synchronized void disposeDummyParent(final String componentId) {
        var dummyParent = compositeById.get(componentId);

        if (dummyParent != null && !dummyParent.isDisposed()) {
            dummyParent.dispose();
            dummyParent = null;
        }
    }

    public synchronized void disposeBrowser(final String componentId) {
        var browser = getBrowser(componentId);

        if (browser != null && !browser.isDisposed()) {
            browser.dispose();
            browser = null;
        }
        disposeDummyParent(componentId);
    }

    /*
     * Gets the status of WebView dependency compatibility
     *
     * @return true if the last check found a compatible WebView, false otherwise
     */
    public synchronized boolean hasWebViewDependency() {
        return this.hasWebViewDependency;
    }

    public synchronized void publishBrowserCompatibilityState() {
        Display.getDefault().asyncExec(() -> {
            Display display = Display.getDefault();
            Shell shell = display.getActiveShell();
            if (shell == null) {
                shell = new Shell(display);
            }

            Composite parent = new Composite(shell, SWT.NONE);
            parent.setVisible(false);

            setupBrowser(parent, "initBrowser", true);
            parent.dispose();
        });
    }

    public void dispose() {
        browserById.forEach((key, value) -> {
            disposeBrowser(key);
        });

        compositeById.forEach((key, value) -> {
            disposeDummyParent(key);
        });
    }

    public static final class Builder {
        private PluginPlatform pluginPlatform;

        public Builder withPluginPlatform(final PluginPlatform pluginPlatform) {
            this.pluginPlatform = pluginPlatform;
            return this;
        }

        public AmazonQBrowserProvider build() {
            if (this.pluginPlatform == null) {
                this.pluginPlatform = PluginUtils.getPlatform();
            }
            return new AmazonQBrowserProvider(this);
        }
    }
}
