// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewSite;

import io.reactivex.rxjava3.disposables.Disposable;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.providers.browser.AmazonQBrowserProvider;
import software.aws.toolkits.eclipse.amazonq.util.ThemeDetector;
import software.aws.toolkits.eclipse.amazonq.views.actions.AmazonQCommonActions;

public abstract class AmazonQView extends BaseAmazonQView {

    private AmazonQBrowserProvider browserProvider;
    private AmazonQCommonActions amazonQCommonActions;
    private static final ThemeDetector THEME_DETECTOR = new ThemeDetector();

    private Disposable signOutActionAuthStateSubscription;
    private Disposable feedbackDialogAuthStateSubscription;
    private Disposable customizationDialogAuthStateSubscription;
    private Disposable toggleAutoTriggerAuthStateSubscription;

    private IViewSite viewSite;

    protected AmazonQView() {
        this.browserProvider = new AmazonQBrowserProvider();
    }

    public final Browser getBrowser() {
        return browserProvider.getBrowser();
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
        return browserProvider.setupBrowser(parent);
    }

    protected final void updateBrowser(final Browser browser) {
        browserProvider.updateBrowser(browser);
    }

    @Override
    public Composite setupView(final Composite parent) {
        Browser browser = getBrowser();

        if (browser != null && !browser.isDisposed()) {
            setupBrowserBackground(parent);
            setupActions();
            setupAuthStatusListeners();
            disableBrowserContextMenu();
        }

        return parent;
    }

    protected final void disableBrowserContextMenu() {
        getBrowser().execute("document.oncontextmenu = e => e.preventDefault();");
    }

    private void setupBrowserBackground(final Composite parent) {
        var bgColor = parent.getBackground();
        getBrowser().setBackground(bgColor);
    }

    private void setupActions() {
        amazonQCommonActions = new AmazonQCommonActions(viewSite);
    }

    private void setupAuthStatusListeners() {
        signOutActionAuthStateSubscription = Activator.getEventBroker().subscribe(AuthState.class,
                amazonQCommonActions.getSignoutAction());
        feedbackDialogAuthStateSubscription = Activator.getEventBroker().subscribe(AuthState.class,
                amazonQCommonActions.getFeedbackDialogContributionAction());
        customizationDialogAuthStateSubscription = Activator.getEventBroker().subscribe(AuthState.class,
                amazonQCommonActions.getCustomizationDialogContributionAction());
        toggleAutoTriggerAuthStateSubscription = Activator.getEventBroker().subscribe(AuthState.class,
                amazonQCommonActions.getToggleAutoTriggerContributionAction());
    }

    public void setViewSite(final IViewSite viewSite) {
        this.viewSite = viewSite;
    }

    public final void setFocus() {
        if (!browserProvider.hasWebViewDependency()) {
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
        if (signOutActionAuthStateSubscription != null && !signOutActionAuthStateSubscription.isDisposed()) {
            signOutActionAuthStateSubscription.dispose();
        }
        if (feedbackDialogAuthStateSubscription != null && !feedbackDialogAuthStateSubscription.isDisposed()) {
            feedbackDialogAuthStateSubscription.dispose();
        }
        if (customizationDialogAuthStateSubscription != null
                && !customizationDialogAuthStateSubscription.isDisposed()) {
            customizationDialogAuthStateSubscription.dispose();
        }
        if (toggleAutoTriggerAuthStateSubscription != null && !toggleAutoTriggerAuthStateSubscription.isDisposed()) {
            toggleAutoTriggerAuthStateSubscription.dispose();
        }
    }

}
