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

import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginDetails;
import software.aws.toolkits.eclipse.amazonq.util.AuthStatusChangedListener;
import software.aws.toolkits.eclipse.amazonq.util.DefaultLoginService;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
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
            Activator.getLogger().error("Failed to show view. You must add the view " + viewId + " to AMAZON_Q_VIEWS Set");
            return;
        }

        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        if (page != null) {
            // Show requested view
            try {
                page.showView(viewId);
                Activator.getLogger().info("Showing view " + viewId);
            } catch (Exception e) {
                Activator.getLogger().error("Error occurred while showing view " + viewId, e);
            }

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
        }
    }

    public final Browser getBrowser() {
        return browser;
    }

    public final AmazonQCommonActions getAmazonQCommonActions() {
        return amazonQCommonActions;
    }

    protected abstract void handleAuthStatusChange(LoginDetails loginDetails);

    protected final void setupAmazonQView(final Composite parent, final LoginDetails loginDetails) {
        setupBrowser(parent);
        setupActions(browser, loginDetails);
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

    private void setupActions(final Browser browser, final LoginDetails loginDetails) {
        amazonQCommonActions = new AmazonQCommonActions(browser, loginDetails, getViewSite());
    }

    private void setupAuthStatusListeners() {
        authStatusChangedListener = this::handleAuthStatusChange;
        DefaultLoginService.addAuthStatusChangeListener(amazonQCommonActions.getSignoutAction());
        DefaultLoginService.addAuthStatusChangeListener(amazonQCommonActions.getFeedbackDialogContributionAction());
        DefaultLoginService.addAuthStatusChangeListener(amazonQCommonActions.getCustomizationDialogContributionAction());
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
        DefaultLoginService.removeAuthStatusChangeListener(authStatusChangedListener);
        super.dispose();
    }

}
