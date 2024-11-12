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
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import software.aws.toolkits.eclipse.amazonq.chat.ChatStateManager;
import software.aws.toolkits.eclipse.amazonq.controllers.AmazonQViewController;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.AuthStatusChangedListener;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.AuthStatusProvider;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.views.actions.AmazonQCommonActions;

public abstract class AmazonQView extends ViewPart implements AuthStatusChangedListener {

    private static final Set<String> AMAZON_Q_VIEWS = Set.of(
            ToolkitLoginWebview.ID,
            AmazonQChatWebview.ID,
            DependencyMissingView.ID,
            ReauthenticateView.ID,
            ChatAssetMissingView.ID
        );
    private AmazonQViewController viewController;
    private AmazonQCommonActions amazonQCommonActions;

    protected AmazonQView() {
        this.viewController = new AmazonQViewController();
    }

    public static void showView(final String viewId) {
        if (!AMAZON_Q_VIEWS
        .contains(viewId)) {
            Activator.getLogger().error("Failed to show view. You must add the view " + viewId + " to AMAZON_Q_VIEWS Set");
            return;
        }

        Display.getDefault().execute(() -> {
            IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            if (window != null) {
                IWorkbenchPage page = window.getActivePage();
                if (page != null) {
                    // Hide all other Amazon Q Views
                    IViewReference[] viewReferences = page.getViewReferences();
                    for (IViewReference viewRef : viewReferences) {
                        if (AMAZON_Q_VIEWS.contains(viewRef.getId()) && !viewRef.getId().equalsIgnoreCase(viewId)) {
                            // if Q chat view is being hidden to show a different Amazon Q view
                            // clear chat preserved state
                            if (viewRef.getId().equalsIgnoreCase(AmazonQChatWebview.ID)) {
                                ChatStateManager.getInstance().dispose();
                            }
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
        });
    }

    public final Browser getBrowser() {
        return viewController.getBrowser();
    }

    public final AmazonQCommonActions getAmazonQCommonActions() {
        return amazonQCommonActions;
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
    }

    private void setupBrowserBackground(final Composite parent) {
        Display display = Display.getCurrent();
        Color black = display.getSystemColor(SWT.COLOR_BLACK);
        parent.setBackground(black);
        getBrowser().setBackground(black);
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

    private void setupActions(final AuthState authState) {
        amazonQCommonActions = new AmazonQCommonActions(authState, getViewSite());
    }

    private void setupAuthStatusListeners() {
        AuthStatusProvider.addAuthStatusChangeListener(this);
        AuthStatusProvider.addAuthStatusChangeListener(amazonQCommonActions.getSignoutAction());
        AuthStatusProvider.addAuthStatusChangeListener(amazonQCommonActions.getFeedbackDialogContributionAction());
        AuthStatusProvider.addAuthStatusChangeListener(amazonQCommonActions.getCustomizationDialogContributionAction());
    }

    @Override
    public final void setFocus() {
        if (!viewController.hasWebViewDependency()) {
            return;
        }
        getBrowser().setFocus();
    }

    /**
     * Disposes of the resources associated with this view.
     *
     * This method is called when the view is closed. It removes the authentication
     * status change listener and the selection listener from the page.
     */
    @Override
    public void dispose() {
        AuthStatusProvider.removeAuthStatusChangeListener(this);
        super.dispose();
    }

}
