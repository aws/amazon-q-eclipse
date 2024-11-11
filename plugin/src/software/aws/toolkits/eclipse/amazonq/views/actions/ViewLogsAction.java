// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.actions;

import org.eclipse.jface.action.Action;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;
import software.aws.toolkits.eclipse.amazonq.telemetry.UiTelemetryProvider;

public final class ViewLogsAction extends Action {

    private static final String LOG_VIEW_ID = "org.eclipse.pde.runtime.LogView";

    public ViewLogsAction() {
        setText("View Logs");
    }

    @Override
    public void run() {
        UiTelemetryProvider.emitClickEventMetric("amazonq_openErrorLog");
        PluginUtils.showView(LOG_VIEW_ID);
    }
}
