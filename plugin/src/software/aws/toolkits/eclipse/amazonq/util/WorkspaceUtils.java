// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

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

}
