// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.views.AmazonQView;
import software.aws.toolkits.eclipse.amazonq.views.ViewVisibilityManager;

public class QOpenLoginViewHandler extends AbstractHandler {

    @Override
    public final Object execute(final ExecutionEvent event) {
        closeAnyOpenQViews();

        if (Activator.getLoginService().getAuthState().isLoggedIn()) {
            ViewVisibilityManager.showChatView("statusBar");
        } else {
            ViewVisibilityManager.showLoginView("statusBar");
        }
        return null;
    }

    public final void closeAnyOpenQViews() {
        IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();

        for (IWorkbenchWindow window : windows) {
            IWorkbenchPage[] pages = window.getPages();

            for (IWorkbenchPage page : pages) {
                IViewReference[] viewReferences = page.getViewReferences();

                for (IViewReference viewRef : viewReferences) {
                    IViewPart view = viewRef.getView(false);

                    if (view instanceof AmazonQView) {
                        page.hideView(view);
                    }
                }
            }
        }

        if (activeWindow != null) {
            activeWindow.getShell().setActive();
        }
    }

}
