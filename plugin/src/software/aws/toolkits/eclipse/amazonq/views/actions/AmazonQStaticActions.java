// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

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
        fillLocalPullDown(bars.getMenuManager());
    }

    private void fillLocalPullDown(final IMenuManager manager) {
        IMenuManager feedbackSubMenu = new MenuManager("Feedback");
        feedbackSubMenu.add(reportAnIssueAction);

        IMenuManager helpSubMenu = new MenuManager("Help");
        helpSubMenu.add(openUserGuideAction);
        helpSubMenu.add(new Separator());
        helpSubMenu.add(viewSourceAction);
        helpSubMenu.add(viewLogsAction);

        manager.add(feedbackSubMenu);
        manager.add(helpSubMenu);
    }

}
