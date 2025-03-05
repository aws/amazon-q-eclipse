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

import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;

public final class AmazonQCommonActions {
    private final Actions actions;
    private AbstractContributionFactory factory;

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

    public AmazonQCommonActions(final AuthState authState, final IViewSite viewSite) {
        actions = new Actions(viewSite);
        fillLocalPullDown(viewSite.getActionBars().getMenuManager());
        updateActionVisibility(authState, viewSite);
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

    private void fillGlobalToolBar() {
        final IMenuService menuService = (IMenuService) PlatformUI.getWorkbench().getService(IMenuService.class);
        var contributionFactory = new MenuContributionFactory("software.aws.toolkits.eclipse.amazonq.toolbar.command");

        IMenuManager tempManager = new MenuManager();
        tempManager.add(actions.openQChatAction);
        addCommonMenuItems(tempManager);

        for (IContributionItem item : tempManager.getItems()) {
            if (item.isVisible()) {
                contributionFactory.addContributionItem(item);
            }
        }

        menuService.addContributionFactory(contributionFactory);
        this.factory = contributionFactory;
    }

    private void addCommonMenuItems(final IMenuManager manager) {
        IMenuManager feedbackSubMenu = new MenuManager("Feedback");
        feedbackSubMenu.add(actions.reportAnIssueAction);
        feedbackSubMenu.add(actions.feedbackDialogContributionItem.getDialogContributionItem());

        IMenuManager helpSubMenu = new MenuManager("Help");
        helpSubMenu.add(actions.openUserGuideAction);
        helpSubMenu.add(new Separator());
        helpSubMenu.add(actions.viewSourceAction);
        helpSubMenu.add(actions.viewLogsAction);

        manager.add(actions.openCodeReferenceLogAction);
        manager.add(new Separator());
        manager.add(actions.toggleAutoTriggerContributionItem);
        manager.add(actions.customizationDialogContributionItem);
        manager.add(new Separator());
        manager.add(feedbackSubMenu);
        manager.add(helpSubMenu);
        manager.add(new Separator());
        manager.add(actions.signoutAction);
    }

    public void updateActionVisibility(final AuthState authState, final IViewSite viewSite) {
        actions.signoutAction.updateVisibility(authState);
        actions.feedbackDialogContributionItem.updateVisibility(authState);
        actions.customizationDialogContributionItem.updateVisibility(authState);
        actions.toggleAutoTriggerContributionItem.updateVisibility(authState);
        Display.getDefault().asyncExec(() -> {
            viewSite.getActionBars().getMenuManager().markDirty();
            viewSite.getActionBars().getMenuManager().update(true);

            final IMenuService menuService = (IMenuService) PlatformUI.getWorkbench().getService(IMenuService.class);
            if (factory != null) {
                menuService.removeContributionFactory(factory);
            }
            fillGlobalToolBar();
        });
    }
}
