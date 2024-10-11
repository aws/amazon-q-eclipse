// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.views.actions;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewSite;

import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginDetails;

import org.eclipse.swt.browser.Browser;

public final class AmazonQCommonActions {

    private ChangeThemeAction changeThemeAction;
    private SignoutAction signoutAction;
    private FeedbackDialogContributionItem feedbackDialogContributionItem;
    private CustomizationDialogContributionItem customizationDialogContributionItem;
    private ToggleAutoTriggerContributionItem toggleAutoTriggerContributionItem;

    public AmazonQCommonActions(final Browser browser, final LoginDetails loginDetails, final IViewSite viewSite) {
        createActions(browser, loginDetails, viewSite);
        contributeToActionBars(viewSite);
        updateActionVisibility(loginDetails, viewSite);
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

    public ToggleAutoTriggerContributionItem getToggleAutoTriggerContributionAction() {
        return toggleAutoTriggerContributionItem;
    }

    private void createActions(final Browser browser, final LoginDetails loginDetails, final IViewSite viewSite) {
        changeThemeAction = new ChangeThemeAction(browser);
        signoutAction = new SignoutAction();
        feedbackDialogContributionItem = new FeedbackDialogContributionItem(viewSite);
        customizationDialogContributionItem = new CustomizationDialogContributionItem(viewSite);
        toggleAutoTriggerContributionItem = new ToggleAutoTriggerContributionItem(viewSite);
    }

    private void contributeToActionBars(final IViewSite viewSite) {
        IActionBars bars = viewSite.getActionBars();
        fillLocalPullDown(bars.getMenuManager());
        fillLocalToolBar(bars.getToolBarManager());
    }

    private void fillLocalPullDown(final IMenuManager manager) {
        manager.add(changeThemeAction);
        manager.add(toggleAutoTriggerContributionItem);
        manager.add(customizationDialogContributionItem);
        manager.add(feedbackDialogContributionItem.getDialogContributionItem());
        manager.add(signoutAction);
    }

    private void fillLocalToolBar(final IToolBarManager manager) {
        manager.add(changeThemeAction);
    }

    public void updateActionVisibility(final LoginDetails loginDetails, final IViewSite viewSite) {
        signoutAction.updateVisibility(loginDetails);
        feedbackDialogContributionItem.updateVisibility(loginDetails);
        customizationDialogContributionItem.updateVisibility(loginDetails);
        toggleAutoTriggerContributionItem.updateVisibility(loginDetails);
    }

}
