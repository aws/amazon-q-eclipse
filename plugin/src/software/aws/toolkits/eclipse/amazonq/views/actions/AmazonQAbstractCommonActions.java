// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.actions;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;

import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;

public abstract class AmazonQAbstractCommonActions {

    protected static final class Actions {
        private final SignoutAction signoutAction;
        private final FeedbackDialogContributionItem feedbackDialogContributionItem;
        private final CustomizationDialogContributionItem customizationDialogContributionItem;
        private final ToggleAutoTriggerContributionItem toggleAutoTriggerContributionItem;
        private final OpenQChatAction openQChatAction;
        private final OpenCodeReferenceLogAction openCodeReferenceLogAction;
        private final OpenUserGuideAction openUserGuideAction;
        private final ViewSourceAction viewSourceAction;
        private final ViewLogsAction viewLogsAction;
        private final ChangeProfileDialogContributionItem changeProfileDialogContributionItem;
        private final ReportAnIssueAction reportAnIssueAction;
        private final OpenPreferencesAction openPreferencesAction;

        Actions() {
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
            openPreferencesAction = new OpenPreferencesAction();
            changeProfileDialogContributionItem = new ChangeProfileDialogContributionItem();
        }

        public OpenQChatAction getOpenQChatAction() {
            return openQChatAction;
        }

        public void setVisibility(final AuthState authState) {
            Display.getDefault().asyncExec(() -> {
                signoutAction.setVisible(authState.isLoggedIn());
                feedbackDialogContributionItem.setVisible(authState.isLoggedIn());
                toggleAutoTriggerContributionItem.setVisible(authState.isLoggedIn());

                // TODO: Need to update this method as the login condition has to be Pro login
                // using IAM identity center
                customizationDialogContributionItem.setVisible(
                        authState.isLoggedIn() && authState.loginType().equals(LoginType.IAM_IDENTITY_CENTER));
                changeProfileDialogContributionItem.setVisible(
                        authState.isLoggedIn() && authState.loginType().equals(LoginType.IAM_IDENTITY_CENTER));
            });
        }

        public void dispose() {
            toggleAutoTriggerContributionItem.dispose();
        }
    }

    protected abstract void fillPulldown();

    protected final void addCommonMenuItems(final IMenuManager menuManager, final Actions action,
            final boolean includeToggleAutoTriggerContributionItem) {
        IMenuManager feedbackSubMenu = new MenuManager("Feedback");
        feedbackSubMenu.add(action.reportAnIssueAction);
        feedbackSubMenu.add(action.feedbackDialogContributionItem.getDialogContributionItem());

        IMenuManager helpSubMenu = new MenuManager("Help");
        helpSubMenu.add(action.openUserGuideAction);
        helpSubMenu.add(new Separator());
        helpSubMenu.add(action.viewSourceAction);
        helpSubMenu.add(action.viewLogsAction);

        menuManager.add(action.openCodeReferenceLogAction);
        menuManager.add(new Separator());

        if (includeToggleAutoTriggerContributionItem) {
            menuManager.add(action.toggleAutoTriggerContributionItem);
        }
        menuManager.add(new ContributionItem(action.customizationDialogContributionItem.getId()) {
            @Override
            public boolean isVisible() {
                return action.customizationDialogContributionItem.isVisible();
            }

            @Override
            public void fill(final Menu parent, final int index) {
                action.customizationDialogContributionItem.fill(parent, index);
            }

            @Override
            public void fill(final Composite parent) {
                action.customizationDialogContributionItem.fill(parent);
            }

            @Override
            public void fill(final ToolBar parent, final int index) {
                action.customizationDialogContributionItem.fill(parent, index);
            }
        });
        menuManager.add(new Separator());
        menuManager.add(action.openPreferencesAction);
        menuManager.add(feedbackSubMenu);
        menuManager.add(helpSubMenu);
        menuManager.add(new Separator());
        menuManager.add(new ContributionItem(action.changeProfileDialogContributionItem.getId()) {
            @Override
            public boolean isVisible() {
                return action.changeProfileDialogContributionItem.isVisible();
            }

            @Override
            public void fill(final Menu parent, final int index) {
                action.changeProfileDialogContributionItem.fill(parent, index);
            }

            @Override
            public void fill(final Composite parent) {
                action.changeProfileDialogContributionItem.fill(parent);
            }

            @Override
            public void fill(final ToolBar parent, final int index) {
                action.changeProfileDialogContributionItem.fill(parent, index);
            }
        });
        menuManager.add(new ActionContributionItem(action.signoutAction) {
            @Override
            public boolean isVisible() {
                return action.signoutAction.isEnabled();
            }
        });
    }

}
