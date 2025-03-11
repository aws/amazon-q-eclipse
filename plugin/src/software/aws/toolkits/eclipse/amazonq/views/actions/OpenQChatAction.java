// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.actions;

import org.eclipse.jface.action.Action;

import software.aws.toolkits.eclipse.amazonq.telemetry.UiTelemetryProvider;
import software.aws.toolkits.eclipse.amazonq.views.ViewVisibilityManager;

public class OpenQChatAction extends Action {

    public OpenQChatAction() {
        setText("Open Q Chat");
    }

    @Override
    public final void run() {
        UiTelemetryProvider.emitClickEventMetric("ellipses_openQChat");
        ViewVisibilityManager.showDefaultView("ellipsesMenu");
    }

}
