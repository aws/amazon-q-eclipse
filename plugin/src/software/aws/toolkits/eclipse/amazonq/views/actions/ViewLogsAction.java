// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.views.actions;


import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

public class ViewLogsAction extends Action {

    String AMAZON_Q_PLUGIN_ID = "amazon-q-eclipse";

    public ViewLogsAction() {
        setText("View Logs");
    }

    @Override
    public void run() {
		openErrorLog();
    }

    private void openErrorLog() {
        try {
            IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            if (window != null) {
                IWorkbenchPage page = window.getActivePage();
                if (page != null) {
                    page.showView("org.eclipse.pde.runtime.LogView");
                }
            }
        } catch (PartInitException e) {
        	Activator.getLogger().error("Failed to open Error Log view", e);
        }
    }
}
