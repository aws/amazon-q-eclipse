// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.views.actions;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewSite;
import org.eclipse.swt.browser.Browser;

public final class AmazonQCommonActions {

    private ChangeThemeAction changeThemeAction;
    private SignoutAction signoutAction;
    private FeedbackDialogContributionItem feedbackDialogContributionItem;
    private CustomizationDialogContributionItem customizationDialogContributionItem;

    public AmazonQCommonActions(final Browser browser, final boolean isLoggedIn, final IViewSite viewSite) {
        createActions(browser, isLoggedIn, viewSite);
        contributeToActionBars(viewSite);
        updateActionVisibility(isLoggedIn, viewSite);
    }

    public ChangeThemeAction getChangeThemeAction() {
        return changeThemeAction;
    }

    public SignoutAction getSignoutAction() {
        return signoutAction;
    }

    public FeedbackDialogContributionItem getFeedbackDialogContributionAction() {
        return feedbackDialogContributionItem;
    }

    public CustomizationDialogContributionItem getCustomizationDialogContributionAction() {
        return customizationDialogContributionItem;
    }

    private void createActions(final Browser browser, final boolean isLoggedIn, final IViewSite viewSite) {
        changeThemeAction = new ChangeThemeAction(browser);
        signoutAction = new SignoutAction();
        feedbackDialogContributionItem = new FeedbackDialogContributionItem(viewSite);
        customizationDialogContributionItem = new CustomizationDialogContributionItem(viewSite);
    }

    private void contributeToActionBars(final IViewSite viewSite) {
        IActionBars bars = viewSite.getActionBars();
        fillLocalPullDown(bars.getMenuManager());
        fillLocalToolBar(bars.getToolBarManager());
    }

    private void fillLocalPullDown(final IMenuManager manager) {
        manager.add(changeThemeAction);
        manager.add(customizationDialogContributionItem);
        manager.add(feedbackDialogContributionItem.getDialogContributionItem());
        manager.add(signoutAction);
    }

    private void fillLocalToolBar(final IToolBarManager manager) {
        manager.add(changeThemeAction);
    }

    public void updateActionVisibility(final boolean isLoggedIn, final IViewSite viewSite) {
        signoutAction.updateVisibility(isLoggedIn);
        feedbackDialogContributionItem.updateVisibility(isLoggedIn);
        customizationDialogContributionItem.updateVisibility(isLoggedIn);
    }

}
