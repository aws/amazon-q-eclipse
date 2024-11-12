// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.part.ViewPart;

import software.aws.toolkits.eclipse.amazonq.controllers.AmazonQViewController;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.AuthStatusChangedListener;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.AuthStatusProvider;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.views.actions.AmazonQCommonActions;

public abstract class AmazonQView extends ViewPart implements AuthStatusChangedListener {

    private AmazonQViewController viewController;
    private AmazonQCommonActions amazonQCommonActions;

    protected AmazonQView() {
        this.viewController = new AmazonQViewController();
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
                ViewVisibilityManager.showDependencyMissingView();
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
