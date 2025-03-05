// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.views.actions;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.menus.AbstractContributionFactory;
import org.eclipse.ui.menus.IMenuService;

public final class AmazonQCommonActions {
    private final Actions actions;
    private AbstractContributionFactory factory;
    private IMenuManager menuManager;

    private static class Actions {
        private final SignoutAction signoutAction;
        private final FeedbackDialogContributionItem feedbackDialogContributionItem;
        private final CustomizationDialogContributionItem customizationDialogContributionItem;
        private final ToggleAutoTriggerContributionItem toggleAutoTriggerContributionItem;
        private final OpenQChatAction openQChatAction;
        private final OpenCodeReferenceLogAction openCodeReferenceLogAction;
        private final OpenUserGuideAction openUserGuideAction;
        private final ViewSourceAction viewSourceAction;
        private final ViewLogsAction viewLogsAction;
        private final ReportAnIssueAction reportAnIssueAction;

        Actions(final IViewSite viewSite) {
            signoutAction = new SignoutAction();
            feedbackDialogContributionItem = new FeedbackDialogContributionItem();
            customizationDialogContributionItem = new CustomizationDialogContributionItem();
            toggleAutoTriggerContributionItem = new ToggleAutoTriggerContributionItem();
            openCodeReferenceLogAction = new OpenCodeReferenceLogAction();
            openQChatAction = new OpenQChatAction();
            openUserGuideAction = new OpenUserGuideAction();
            viewSourceAction = new ViewSourceAction();
            viewLogsAction = new ViewLogsAction();
            reportAnIssueAction = new ReportAnIssueAction();
        }
    }

    public AmazonQCommonActions(final IViewSite viewSite) {
        createActions(viewSite);
        contributeToActionBars(viewSite);
    }

    public SignoutAction getSignoutAction() {
        return actions.signoutAction;
    }

    public FeedbackDialogContributionItem getFeedbackDialogContributionAction() {
        return actions.feedbackDialogContributionItem;
    }

    public CustomizationDialogContributionItem getCustomizationDialogContributionAction() {
        return actions.customizationDialogContributionItem;
    }

    public ToggleAutoTriggerContributionItem getToggleAutoTriggerContributionAction() {
        return actions.toggleAutoTriggerContributionItem;
    }

    private void fillLocalPullDown(final IMenuManager manager) {
        addCommonMenuItems(manager);
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
        feedbackSubMenu.add(actions.reportAnIssueAction);
        feedbackSubMenu.add(actions.feedbackDialogContributionItem.getDialogContributionItem());

        IMenuManager helpSubMenu = new MenuManager("Help");
        helpSubMenu.add(actions.openUserGuideAction);
        helpSubMenu.add(new Separator());
        helpSubMenu.add(actions.viewSourceAction);
        helpSubMenu.add(actions.viewLogsAction);

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
