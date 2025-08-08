// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

public final class WorkspaceUtils {

    private WorkspaceUtils() { }

    public static void refreshAllProjects() {
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        for (IProject project : projects) {
            try {
                project.refreshLocal(IResource.DEPTH_INFINITE, null);
            } catch (CoreException e) {
                Activator.getLogger().warn("Failed to refresh project(s): " + e.getMessage());
            }
        }
    }

    public static void refreshAdtViews() {
        try {
            IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            if (window == null) {
                return;
            }

            IWorkbenchPage page = window.getActivePage();
            if (page == null) {
                return;
            }

            IViewPart adtView = page.findView("com.sap.adt.tools.core.ui.views.objectnavigator");
            if (adtView != null) {
                // Force refresh of ADT view which triggers server sync
                adtView.getSite().getPage().activate(adtView);

                // Send refresh command to ADT view
                var handlerService = adtView.getSite().getService(IHandlerService.class);
                if (handlerService != null) {
                    handlerService.executeCommand("org.eclipse.ui.file.refresh", null);
                }
            }
        } catch (Exception e) {
            Activator.getLogger().error("Failed to refresh ADT views", e);
        }
    }

}
