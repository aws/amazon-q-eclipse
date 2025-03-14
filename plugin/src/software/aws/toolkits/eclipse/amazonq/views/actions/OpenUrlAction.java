// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.actions;

import org.eclipse.jface.action.Action;

import software.aws.toolkits.eclipse.amazonq.telemetry.UiTelemetryProvider;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;
import software.aws.toolkits.eclipse.amazonq.views.model.ExternalLink;

public class OpenUrlAction extends Action {
    private String url;
    private String metadataId;

    public OpenUrlAction(final String actionText, final ExternalLink link) {
        setText(actionText);
        this.url = link.getValue();
    }

    public OpenUrlAction(final String actionText, final String metadataId, final ExternalLink link) {
        setText(actionText);
        this.url = link.getValue();
        this.metadataId = metadataId;
    }

    @Override
    public final void run() {
        UiTelemetryProvider.emitClickEventMetric(this.metadataId);
        PluginUtils.openWebpage(this.url);
    }
}
