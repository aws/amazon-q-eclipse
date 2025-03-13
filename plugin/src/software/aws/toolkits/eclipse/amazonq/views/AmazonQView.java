// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.part.ViewPart;

import io.reactivex.rxjava3.disposables.Disposable;
import software.aws.toolkits.eclipse.amazonq.broker.api.EventObserver;
import software.aws.toolkits.eclipse.amazonq.controllers.AmazonQViewController;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.ThemeDetector;
import software.aws.toolkits.eclipse.amazonq.views.actions.AmazonQCommonActions;

public abstract class AmazonQView extends ViewPart implements EventObserver<AuthState> {

    private AmazonQViewController viewController;
    private AmazonQCommonActions amazonQCommonActions;
    private static final ThemeDetector THEME_DETECTOR = new ThemeDetector();

    private Disposable authStateSubscription;

    protected AmazonQView() {
        this.viewController = new AmazonQViewController();
    }

    public final Browser getBrowser() {
        return viewController.getBrowser();
    }

    public final AmazonQCommonActions getAmazonQCommonActions() {
        return amazonQCommonActions;
    }

    protected final void setupParentBackground(final Composite parent) {
        Display display = Display.getCurrent();
        Color bg = THEME_DETECTOR.isDarkTheme() ? display.getSystemColor(SWT.COLOR_BLACK)
                : display.getSystemColor(SWT.COLOR_WHITE);
        parent.setBackground(bg);
    }

    protected final boolean setupBrowser(final Composite parent) {
        return viewController.setupBrowser(parent);
    }

    protected final void updateBrowser(final Browser browser) {
        viewController.updateBrowser(browser);
    }

    protected final void setupAmazonQView(final Composite parent, final AuthState authState) {
        setupBrowserBackground(parent);
        setupActions(authState);
        setupAuthStatusListeners();
        disableBrowserContextMenu();
    }

    protected final void disableBrowserContextMenu() {
        getBrowser().execute("document.oncontextmenu = e => e.preventDefault();");
    }

    private void setupBrowserBackground(final Composite parent) {
        var bgColor = parent.getBackground();
        getBrowser().setBackground(bgColor);
    }

    protected final void showDependencyMissingView(final String source) {
        Display.getCurrent().asyncExec(() -> {
            try {
                ViewVisibilityManager.showDependencyMissingView(source);
            } catch (Exception e) {
                Activator.getLogger().error("Error occured while attempting to show missing webview dependencies view", e);
            }
        });
    }

    private void setupActions(final AuthState authState) {
        amazonQCommonActions = new AmazonQCommonActions(authState, getViewSite());
    }

    private void setupAuthStatusListeners() {
        authStateSubscription = Activator.getEventBroker().subscribe(AuthState.class, this);
        Activator.getEventBroker().subscribe(AuthState.class, amazonQCommonActions.getSignoutAction());
        Activator.getEventBroker().subscribe(AuthState.class, amazonQCommonActions.getFeedbackDialogContributionAction());
        Activator.getEventBroker().subscribe(AuthState.class, amazonQCommonActions.getCustomizationDialogContributionAction());
    }

    @Override
    public final void setFocus() {
        if (!viewController.hasWebViewDependency()) {
            return;
        }
        getBrowser().setFocus();
    }

    protected final String getWaitFunction() {
        return """
                function waitForFunction(functionName, timeout = 30000) {
                    return new Promise((resolve, reject) => {
                        const startTime = Date.now();
                        const checkFunction = () => {
                            if (typeof window[functionName] === 'function') {
                                resolve(window[functionName]);
                            } else if (Date.now() - startTime > timeout) {
                                reject(new Error(`Timeout waiting for ${functionName}`));
                            } else {
                                setTimeout(checkFunction, 100);
                            }
                        };
                        checkFunction();
                    });
                }
                """;
    }

    /**
     * Disposes of the resources associated with this view.
     *
     * This method is called when the view is closed. It removes the authentication
     * status change listener and the selection listener from the page.
     */
    @Override
    public void dispose() {
        if (authStateSubscription != null) {
            authStateSubscription.dispose();
            authStateSubscription = null;
        }
        super.dispose();
    }

}
