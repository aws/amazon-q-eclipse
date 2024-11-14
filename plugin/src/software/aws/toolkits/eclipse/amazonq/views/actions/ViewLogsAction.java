// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.actions;

import org.eclipse.jface.action.Action;
import software.aws.toolkits.eclipse.amazonq.views.ViewVisibilityManager;
import software.aws.toolkits.eclipse.amazonq.telemetry.UiTelemetryProvider;

public final class ViewLogsAction extends Action {

    public ViewLogsAction() {
        setText("View Logs");
    }

    @Override
    public void run() {
        UiTelemetryProvider.emitClickEventMetric("ellipses_openErrorLog");
        ViewVisibilityManager.showErrorLogView();
    }
}
