// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.actions;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewSite;

public final class AmazonQStaticActions {

    private OpenUserGuideAction openUserGuideAction;
    private ViewSourceAction viewSourceAction;
    private ViewLogsAction viewLogsAction;
    private ReportAnIssueAction reportAnIssueAction;

    private IMenuManager menuManager;

    public AmazonQStaticActions(final IViewSite viewSite) {
        createActions(viewSite);
        contributeToActionBars(viewSite);
    }

    private void createActions(final IViewSite viewSite) {
        openUserGuideAction = new OpenUserGuideAction();
        viewSourceAction = new ViewSourceAction();
        viewLogsAction = new ViewLogsAction();
        reportAnIssueAction = new ReportAnIssueAction();
    }

    private void contributeToActionBars(final IViewSite viewSite) {
        IActionBars bars = viewSite.getActionBars();
        menuManager = bars.getMenuManager();
        fillLocalPullDown();
    }

    private void fillLocalPullDown() {
        IMenuManager feedbackSubMenu = new MenuManager("Feedback");
        feedbackSubMenu.add(reportAnIssueAction);

        IMenuManager helpSubMenu = new MenuManager("Help");
        helpSubMenu.add(openUserGuideAction);
        helpSubMenu.add(new Separator());
        helpSubMenu.add(viewSourceAction);
        helpSubMenu.add(viewLogsAction);

        menuManager.add(feedbackSubMenu);
        menuManager.add(helpSubMenu);
    }

    public void dispose() {
        if (menuManager != null) {
            menuManager.dispose();
            menuManager = null;
        }
    }

}
