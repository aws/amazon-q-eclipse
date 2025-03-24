// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.telemetry.ToolkitTelemetryProvider;
import software.aws.toolkits.eclipse.amazonq.telemetry.metadata.ExceptionMetadata;

public final class ViewVisibilityManager {
    private ViewVisibilityManager() {
        // prevent instantiation
    }

    private static final String CODE_REFERENCE_VIEW = AmazonQCodeReferenceView.ID;
    private static final String ERROR_LOG_VIEW = "org.eclipse.pde.runtime.LogView";
    private static final String AMAZON_Q_VIEW_CONTAINER = AmazonQViewContainer.ID;


    public static void showDefaultView(final String source) {
        showView(AMAZON_Q_VIEW_CONTAINER, source);
    }

    public static void showCodeReferenceView(final String source) {
        showView(CODE_REFERENCE_VIEW, source);
    }

    public static void showErrorLogView(final String source) {
        showView(ERROR_LOG_VIEW, source);
    }

    private static void showView(final String viewId, final String source) {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
            IWorkbenchPage page = window.getActivePage();
            if (page != null) {
                if (viewIsAlreadyOpen(page, viewId)) {
                    return;
                }
                try {
                    page.showView(viewId);
                    ToolkitTelemetryProvider.emitOpenModuleEventMetric(viewId, source, "none");
                } catch (PartInitException e) {
                    Activator.getLogger().error("Error occurred while opening view " + viewId, e);
                    ToolkitTelemetryProvider.emitOpenModuleEventMetric(viewId, source, ExceptionMetadata.scrubException(e));
                }
            }
        }
    }
    private static boolean viewIsAlreadyOpen(final IWorkbenchPage page, final String viewId) {
        IWorkbenchPartReference activeRef = page.getActivePartReference();
        if (activeRef instanceof IViewReference) {
            IViewReference activeViewId = (IViewReference) activeRef;
            if (activeViewId.getId().equals(viewId)) {
                return true;
            }
        }
        return false;
    }
}
