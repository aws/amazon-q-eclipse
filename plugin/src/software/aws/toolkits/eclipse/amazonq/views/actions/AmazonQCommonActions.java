// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.views.actions;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewSite;

public class AmazonQCommonActions {
    
    public ChangeThemeAction changeThemeAction;
    public SignoutAction signoutAction;
    public FeedbackDialogContributionItem feedbackDialogContributionItem;
    
    public AmazonQCommonActions(final boolean isLoggedIn, IViewSite viewSite) {
        createActions(isLoggedIn, viewSite);
        contributeToActionBars(viewSite);
        updateActionVisibility(isLoggedIn, viewSite);
    }
    
    private void createActions(final boolean isLoggedIn, IViewSite viewSite) {
        changeThemeAction = new ChangeThemeAction();
        signoutAction = new SignoutAction();
        feedbackDialogContributionItem = new FeedbackDialogContributionItem(viewSite);
    }

    private void contributeToActionBars(final IViewSite viewSite) {
        IActionBars bars = viewSite.getActionBars();
        fillLocalPullDown(bars.getMenuManager());
        fillLocalToolBar(bars.getToolBarManager());
    }

    private void fillLocalPullDown(final IMenuManager manager) {
        manager.add(changeThemeAction);
        manager.add(feedbackDialogContributionItem.getDialogContributionItem());
        manager.add(signoutAction);
    }

    private void fillLocalToolBar(final IToolBarManager manager) {
        manager.add(changeThemeAction);
    }

    public void updateActionVisibility(final boolean isLoggedIn, final IViewSite viewSite) {
        signoutAction.updateVisibility(isLoggedIn);
        feedbackDialogContributionItem.updateVisibility(isLoggedIn);
    }
    
}
