// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.views;

import java.util.Set;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import software.aws.toolkits.eclipse.amazonq.controllers.AmazonQViewController;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginDetails;
import software.aws.toolkits.eclipse.amazonq.util.AuthStatusChangedListener;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.views.actions.AmazonQCommonActions;
import software.aws.toolkits.eclipse.amazonq.util.AuthStatusProvider;

public abstract class AmazonQView extends ViewPart {

    private static final Set<String> AMAZON_Q_VIEWS = Set.of(
            ToolkitLoginWebview.ID,
            AmazonQChatWebview.ID,
            DependencyMissingView.ID
        );

    private Browser browser;
    private AmazonQCommonActions amazonQCommonActions;
    private AuthStatusChangedListener authStatusChangedListener;
    private AmazonQViewController viewController;

    protected AmazonQView() {
        this.viewController = new AmazonQViewController();
    }

    public static void showView(final String viewId) {
        if (!AMAZON_Q_VIEWS
        .contains(viewId)) {
            Activator.getLogger().error("Failed to show view. You must add the view " + viewId + " to AMAZON_Q_VIEWS Set");
            return;
        }

        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        if (page != null) {
            // Hide all other Amazon Q Views
            IViewReference[] viewReferences = page.getViewReferences();
            for (IViewReference viewRef : viewReferences) {
                if (AMAZON_Q_VIEWS.contains(viewRef.getId()) && !viewRef.getId().equalsIgnoreCase(viewId)) {
                    try {
                        page.hideView(viewRef);
                    } catch (Exception e) {
                        Activator.getLogger().error("Error occurred while hiding view " + viewId, e);
                    }
                }
            }
            // Show requested view
            try {
                page.showView(viewId);
                Activator.getLogger().info("Showing view " + viewId);
            } catch (Exception e) {
                Activator.getLogger().error("Error occurred while showing view " + viewId, e);
            }
        }
    }

    public final Browser getBrowser() {
        return browser;
    }

    public final AmazonQCommonActions getAmazonQCommonActions() {
        return amazonQCommonActions;
    }

    protected abstract void handleAuthStatusChange(LoginDetails loginDetails);

    protected final boolean setupAmazonQView(final Composite parent, final LoginDetails loginDetails) {
        // if browser setup fails, don't set up rest of the content
        if (!setupBrowser(parent)) {
            return false;
        }
        setupBrowserBackground(parent);
        setupActions(browser, loginDetails);
        setupAuthStatusListeners();
        return true;
    }

    private void setupBrowserBackground(final Composite parent) {
        Display display = Display.getCurrent();
        Color black = display.getSystemColor(SWT.COLOR_BLACK);
        parent.setBackground(black);
        browser.setBackground(black);
    }

    /*
     * Sets up the browser compatible with the platform
     * returns boolean representing whether a browser type compatible with webview rendering for the current platform is found
     * @param parent
     */
    protected final boolean setupBrowser(final Composite parent) {
        var browser = new Browser(parent, viewController.getBrowserStyle());
        viewController.checkWebViewCompatibility(browser.getBrowserType());
        // only set the browser if compatible webview browser can be found for the platform
        if (viewController.hasWebViewDependency()) {
            this.browser = browser;
        }
        return viewController.hasWebViewDependency();
    }

    protected final void showDependencyMissingView() {
        Display.getCurrent().asyncExec(() -> {
            try {
                showView(DependencyMissingView.ID);
            } catch (Exception e) {
                Activator.getLogger().error("Error occured while attempting to show missing webview dependencies view", e);
            }
        });
    }

    private void setupActions(final Browser browser, final LoginDetails loginDetails) {
        amazonQCommonActions = new AmazonQCommonActions(browser, loginDetails, getViewSite());
    }

    private void setupAuthStatusListeners() {
        authStatusChangedListener = this::handleAuthStatusChange;
        AuthStatusProvider.addAuthStatusChangeListener(amazonQCommonActions.getSignoutAction());
        AuthStatusProvider.addAuthStatusChangeListener(amazonQCommonActions.getFeedbackDialogContributionAction());
        AuthStatusProvider.addAuthStatusChangeListener(amazonQCommonActions.getCustomizationDialogContributionAction());
    }

    @Override
    public final void setFocus() {
        if (!viewController.hasWebViewDependency()) {
            return;
        }
        browser.setFocus();
    }

    /**
     * Disposes of the resources associated with this view.
     *
     * This method is called when the view is closed. It removes the authentication
     * status change listener and the selection listener from the page.
     */
    @Override
    public void dispose() {
        AuthStatusProvider.removeAuthStatusChangeListener(authStatusChangedListener);
        super.dispose();
    }

}
