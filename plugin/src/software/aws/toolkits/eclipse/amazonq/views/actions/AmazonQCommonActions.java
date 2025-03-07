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

import io.reactivex.rxjava3.disposables.Disposable;
import software.aws.toolkits.eclipse.amazonq.broker.api.EventObserver;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

public final class AmazonQCommonActions implements EventObserver<AuthState> {
    private final Actions actions;
    private AbstractContributionFactory factory;
    private final IViewSite viewSite;
    private final Disposable authStateSubscription;
    private IMenuManager localActionsMenuManager;

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
        this.viewSite = viewSite;
        localActionsMenuManager = viewSite.getActionBars().getMenuManager();

        actions = new Actions(viewSite);

        fillLocalPullDown();
        authStateSubscription = Activator.getEventBroker().subscribe(AuthState.class, this);
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

    private void fillLocalPullDown() {
        addCommonMenuItems(localActionsMenuManager);
    }

    private void fillGlobalToolBar() {
        final IMenuService menuService = PlatformUI.getWorkbench().getService(IMenuService.class);
        var contributionFactory = new MenuContributionFactory("software.aws.toolkits.eclipse.amazonq.toolbar.command");

        IMenuManager tempMenuManager = new MenuManager();
        tempMenuManager.add(actions.openQChatAction);
        addCommonMenuItems(tempMenuManager);

        for (IContributionItem item : tempMenuManager.getItems()) {
            if (item.isVisible()) {
                contributionFactory.addContributionItem(item);
            }
        }

        menuService.addContributionFactory(contributionFactory);
        this.factory = contributionFactory;
    }

    private void addCommonMenuItems(final IMenuManager menuManager) {
        IMenuManager feedbackSubMenu = new MenuManager("Feedback");
        feedbackSubMenu.add(actions.reportAnIssueAction);
        feedbackSubMenu.add(actions.feedbackDialogContributionItem.getDialogContributionItem());

        IMenuManager helpSubMenu = new MenuManager("Help");
        helpSubMenu.add(actions.openUserGuideAction);
        helpSubMenu.add(new Separator());
        helpSubMenu.add(actions.viewSourceAction);
        helpSubMenu.add(actions.viewLogsAction);

        menuManager.add(actions.openCodeReferenceLogAction);
        menuManager.add(new Separator());
        menuManager.add(actions.toggleAutoTriggerContributionItem);
        menuManager.add(actions.customizationDialogContributionItem);
        menuManager.add(new Separator());
        menuManager.add(feedbackSubMenu);
        menuManager.add(helpSubMenu);
        menuManager.add(new Separator());
        menuManager.add(actions.signoutAction);
    }

    @Override
    public void onEvent(final AuthState authState) {
        actions.signoutAction.setVisible(authState.isLoggedIn());
        actions.feedbackDialogContributionItem.setVisible(authState.isLoggedIn());
        actions.toggleAutoTriggerContributionItem.setVisible(authState.isLoggedIn());

        // TODO: Need to update this method as the login condition has to be Pro login
        // using IAM identity center
        actions.customizationDialogContributionItem.setVisible(authState.isLoggedIn()
                && authState.loginType().equals(LoginType.IAM_IDENTITY_CENTER));

        Display.getDefault().asyncExec(() -> {
            viewSite.getActionBars().getMenuManager().markDirty();
            viewSite.getActionBars().getMenuManager().update(true);

            final IMenuService menuService = PlatformUI.getWorkbench().getService(IMenuService.class);
            if (factory != null) {
                menuService.removeContributionFactory(factory);
            }
            fillGlobalToolBar();
        });
    }

    public void dispose() {
        authStateSubscription.dispose();
        actions.toggleAutoTriggerContributionItem.dispose();

        if (localActionsMenuManager != null) {
            localActionsMenuManager.dispose();
        }

        final IMenuService menuService = PlatformUI.getWorkbench().getService(IMenuService.class);
        if (factory != null) {
            menuService.removeContributionFactory(factory);
        }
    }

}
