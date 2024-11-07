package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;

import software.aws.toolkits.eclipse.amazonq.controllers.AmazonQViewController;

public final class BrowserProvider {
    private AmazonQViewController viewController;
    private Browser browser;

    public BrowserProvider() {
        this.viewController = new AmazonQViewController();
    }

    public Browser getBrowser() {
        return browser;
    }

    /*
     * Must be called after setupBrowser()
     */
    public Boolean hasWebViewDependency() {
        return viewController.hasWebViewDependency();
    }

    /*
     * Sets up the browser compatible with the platform
     * returns boolean representing whether a browser type compatible with webview rendering for the current platform is found
     * @param parent
     */
    public boolean setupBrowser(final Composite parent) {
        var browser = new Browser(parent, viewController.getBrowserStyle());
        viewController.checkWebViewCompatibility(browser.getBrowserType());
        // only set the browser if compatible webview browser can be found for the
        // platform
        if (viewController.hasWebViewDependency()) {
            this.browser = browser;
        }
        return viewController.hasWebViewDependency();
    }

    public void dispose() {
        if (browser != null) {
            browser.dispose();
        }
    }
}
