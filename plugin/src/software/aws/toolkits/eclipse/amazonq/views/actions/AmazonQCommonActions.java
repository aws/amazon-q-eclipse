// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.views.actions;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewSite;


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

    private IMenuManager menuManager;

    public AmazonQCommonActions(final IViewSite viewSite) {
        createActions(viewSite);
        contributeToActionBars(viewSite);
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
        menuManager = bars.getMenuManager();
        IToolBarManager toolBarManager = bars.getToolBarManager();

        menuManager.removeAll();
        toolBarManager.removeAll();
        bars.updateActionBars();

        fillLocalPullDown();
        fillLocalToolBar(toolBarManager);
    }

    private void fillLocalPullDown() {
        IMenuManager feedbackSubMenu = new MenuManager("Feedback");
        feedbackSubMenu.add(reportAnIssueAction);
        feedbackSubMenu.add(feedbackDialogContributionItem.getDialogContributionItem());

        IMenuManager helpSubMenu = new MenuManager("Help");
        helpSubMenu.add(openUserGuideAction);
        helpSubMenu.add(new Separator());
        helpSubMenu.add(viewSourceAction);
        helpSubMenu.add(viewLogsAction);

        menuManager.add(openCodeReferenceLogAction);
        menuManager.add(new Separator());
        menuManager.add(toggleAutoTriggerContributionItem);
        menuManager.add(customizationDialogContributionItem);
        menuManager.add(new Separator());
        menuManager.add(feedbackSubMenu);
        menuManager.add(helpSubMenu);
        menuManager.add(new Separator());
        menuManager.add(signoutAction);
    }

    private void fillLocalToolBar(final IToolBarManager manager) {
        // No actions added to the view toolbar at this time
    }

    public void dispose() {
        if (toggleAutoTriggerContributionItem != null) {
            toggleAutoTriggerContributionItem.dispose();
            toggleAutoTriggerContributionItem = null;
        }

        if (menuManager != null) {
            menuManager.dispose();
            menuManager = null;
        }
    }

}
