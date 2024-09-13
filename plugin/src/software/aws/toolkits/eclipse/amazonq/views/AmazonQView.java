// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.views;


import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import software.aws.toolkits.eclipse.amazonq.util.AuthStatusChangedListener;
import software.aws.toolkits.eclipse.amazonq.util.AuthUtils;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;
import software.aws.toolkits.eclipse.amazonq.views.actions.AmazonQCommonActions;

public abstract class AmazonQView extends ViewPart implements ISelectionListener {
    
    private static final Set<String> amazonQViews = Set.of(
            ToolkitLoginWebview.ID,
            AmazonQChatWebview.ID
        );

    
    protected Browser browser;
    protected AmazonQCommonActions amazonQCommonActions;
    
    private AuthStatusChangedListener authStatusChangedListener;
    
    public static void showView(String viewId) {    
        if (!amazonQViews.contains(viewId)) {
            PluginLogger.error("Failed to show view. You must add the view (" + viewId + ") to amazonQViews Set");
            return;
        }

        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        if (page != null) {
            // Show requested view
            try {
                PluginLogger.info("Attempting to show view" + viewId);
                page.showView(viewId);
            } catch (Exception e) {
                PluginLogger.error("Error occurred while showing view (" + viewId + ")", e);
            }

            // Hide all other Amazon Q Views
            IViewReference[] viewReferences = page.getViewReferences();
            for (IViewReference viewRef : viewReferences) {
                if (amazonQViews.contains(viewRef.getId()) && !viewRef.getId().equalsIgnoreCase(viewId)) {
                    try {
                        page.hideView(viewRef);
                    } catch (Exception e) {
                        PluginLogger.error("Error occurred while hiding view (" + viewId + ")", e);
                    }
                }
            }
        }
    }

    protected abstract void handleAuthStatusChange(final boolean isLoggedIn);
    
    protected void setupAmazonQView(final Composite parent, final boolean isLoggedIn) {
        setupBrowser(parent);
        setupActions(isLoggedIn);
        setupAuthStatusListeners();
        setupSelectionListener();
    }
    
    private void setupBrowser(final Composite parent) {
        browser = new Browser(parent, SWT.NATIVE);
        Display display = Display.getCurrent();
        Color black = display.getSystemColor(SWT.COLOR_BLACK);

        browser.setBackground(black);
        parent.setBackground(black);
    }
    
    private void setupActions(final boolean isLoggedIn) {
        amazonQCommonActions = new AmazonQCommonActions(isLoggedIn, getViewSite());
    }
    
    private void setupAuthStatusListeners() {
        authStatusChangedListener = this::handleAuthStatusChange;
        AuthUtils.addAuthStatusChangeListener(amazonQCommonActions.signoutAction);
        AuthUtils.addAuthStatusChangeListener(amazonQCommonActions.feedbackDialogContributionItem);
    }
    
    private void setupSelectionListener() {
        getSite().getPage().addSelectionListener(this);
    }
    
    @Override
    public final void setFocus() {
        browser.setFocus();
    }
    
    @Override
    public void dispose() {
        AuthUtils.removeAuthStatusChangeListener(authStatusChangedListener);
        getSite().getPage().removeSelectionListener(this);
        super.dispose();
    }

}