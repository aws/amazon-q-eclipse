// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.views.AmazonQCodeReferenceView;

public final class OpenCodeReferenceLogAction extends Action {

 public OpenCodeReferenceLogAction() {
     setText("Open Code Reference Log");
 }

 @Override
 public void run() {
     IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
            IWorkbenchPage page = window.getActivePage();
            if (page != null) {
                try {
                    page.showView(AmazonQCodeReferenceView.ID);
                } catch (PartInitException e) {
                    Activator.getLogger().error("Error occurred while opening Amazon Q Code Reference view", e);
                }
            }
        }
     }
}
