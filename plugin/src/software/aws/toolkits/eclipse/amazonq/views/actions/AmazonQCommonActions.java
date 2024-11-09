// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.views.actions;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewSite;

import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;


public final class AmazonQCommonActions {

    private SignoutAction signoutAction;
    private FeedbackDialogContributionItem feedbackDialogContributionItem;
    private CustomizationDialogContributionItem customizationDialogContributionItem;
    private ToggleAutoTriggerContributionItem toggleAutoTriggerContributionItem;
    private OpenCodeReferenceLogAction openCodeReferenceLogAction;
    private OpenUserGuideAction openUserGuideAction;
    private ViewSourceAction viewSourceAction;
    private ViewLogsAction viewLogsAction;
    private ReportAnIssueAction reportAnIssueAction;

    public AmazonQCommonActions(final AuthState authState, final IViewSite viewSite) {
        createActions(viewSite);
        contributeToActionBars(viewSite);
        updateActionVisibility(authState, viewSite);
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

    private void createActions(final IViewSite viewSite) {
        signoutAction = new SignoutAction();
        feedbackDialogContributionItem = new FeedbackDialogContributionItem(viewSite);
        customizationDialogContributionItem = new CustomizationDialogContributionItem(viewSite);
        toggleAutoTriggerContributionItem = new ToggleAutoTriggerContributionItem(viewSite);
        openUserGuideAction = new OpenUserGuideAction();
        viewSourceAction = new ViewSourceAction();
        viewLogsAction = new ViewLogsAction();
        reportAnIssueAction = new ReportAnIssueAction();
        openCodeReferenceLogAction = new OpenCodeReferenceLogAction();
    }

    private void contributeToActionBars(final IViewSite viewSite) {
        IActionBars bars = viewSite.getActionBars();
        fillLocalPullDown(bars.getMenuManager());
        fillLocalToolBar(bars.getToolBarManager());
    }

    private void fillLocalPullDown(final IMenuManager manager) {
        IMenuManager feedbackSubMenu = new MenuManager("Feedback");
        feedbackSubMenu.add(reportAnIssueAction);
        feedbackSubMenu.add(feedbackDialogContributionItem.getDialogContributionItem());

        IMenuManager helpSubMenu = new MenuManager("Help");
        helpSubMenu.add(openUserGuideAction);
        helpSubMenu.add(new Separator());
        helpSubMenu.add(viewSourceAction);
        helpSubMenu.add(viewLogsAction);

        manager.add(openCodeReferenceLogAction);
        manager.add(new Separator());
        manager.add(toggleAutoTriggerContributionItem);
        manager.add(customizationDialogContributionItem);
        manager.add(new Separator());
        manager.add(feedbackSubMenu);
        manager.add(helpSubMenu);
        manager.add(new Separator());
        manager.add(signoutAction);
    }

    private void fillLocalToolBar(final IToolBarManager manager) {
        // No actions added to the view toolbar at this time
    }

    public void updateActionVisibility(final AuthState authState, final IViewSite viewSite) {
        signoutAction.updateVisibility(authState);
        feedbackDialogContributionItem.updateVisibility(authState);
        customizationDialogContributionItem.updateVisibility(authState);
        toggleAutoTriggerContributionItem.updateVisibility(authState);
    }

}
