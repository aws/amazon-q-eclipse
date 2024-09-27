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

import software.aws.toolkits.eclipse.amazonq.util.AuthStatusChangedListener;
import software.aws.toolkits.eclipse.amazonq.util.AuthUtils;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;
import software.aws.toolkits.eclipse.amazonq.util.PluginPlatform;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;
import software.aws.toolkits.eclipse.amazonq.views.actions.AmazonQCommonActions;

public abstract class AmazonQView extends ViewPart {

    private static final Set<String> AMAZON_Q_VIEWS = Set.of(
            ToolkitLoginWebview.ID,
            AmazonQChatWebview.ID
        );

    private Browser browser;
    private AmazonQCommonActions amazonQCommonActions;
    private AuthStatusChangedListener authStatusChangedListener;

    public static void showView(final String viewId) {
        if (!AMAZON_Q_VIEWS
        .contains(viewId)) {
            PluginLogger.error("Failed to show view. You must add the view " + viewId + " to AMAZON_Q_VIEWS Set");
            return;
        }

        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        if (page != null) {
            // Show requested view
            try {
                page.showView(viewId);
                PluginLogger.info("Showing view " + viewId);
            } catch (Exception e) {
                PluginLogger.error("Error occurred while showing view " + viewId, e);
            }

            // Hide all other Amazon Q Views
            IViewReference[] viewReferences = page.getViewReferences();
            for (IViewReference viewRef : viewReferences) {
                if (AMAZON_Q_VIEWS.contains(viewRef.getId()) && !viewRef.getId().equalsIgnoreCase(viewId)) {
                    try {
                        page.hideView(viewRef);
                    } catch (Exception e) {
                        PluginLogger.error("Error occurred while hiding view " + viewId, e);
                    }
                }
            }
        }
    }

    public final Browser getBrowser() {
        return browser;
    }

    public final AmazonQCommonActions getAmazonQCommonActions() {
        return amazonQCommonActions;
    }

    protected abstract void handleAuthStatusChange(boolean isLoggedIn);

    protected final void setupAmazonQView(final Composite parent, final boolean isLoggedIn) {
        setupBrowser(parent);
        setupActions(browser, isLoggedIn);
        setupAuthStatusListeners();
    }

    private void setupBrowser(final Composite parent) {
        browser = new Browser(parent, getBrowserStyle());
        Display display = Display.getCurrent();
        Color black = display.getSystemColor(SWT.COLOR_BLACK);

        browser.setBackground(black);
        parent.setBackground(black);
    }

    private int getBrowserStyle() {
        var platform = PluginUtils.getPlatform();
        if (platform == PluginPlatform.WINDOWS) {
            return SWT.EDGE;
        }
        return SWT.WEBKIT;
    }

    private void setupActions(final Browser browser, final boolean isLoggedIn) {
        amazonQCommonActions = new AmazonQCommonActions(browser, isLoggedIn, getViewSite());
    }

    private void setupAuthStatusListeners() {
        authStatusChangedListener = this::handleAuthStatusChange;
        AuthUtils.addAuthStatusChangeListener(amazonQCommonActions.getSignoutAction());
        AuthUtils.addAuthStatusChangeListener(amazonQCommonActions.getFeedbackDialogContributionAction());
    }

    @Override
    public final void setFocus() {
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
        AuthUtils.removeAuthStatusChangeListener(authStatusChangedListener);
        super.dispose();
    }

}
