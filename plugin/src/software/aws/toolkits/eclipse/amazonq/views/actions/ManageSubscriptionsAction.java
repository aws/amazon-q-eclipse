// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.lsp4j.ExecuteCommandParams;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.telemetry.UiTelemetryProvider;

public class ManageSubscriptionsAction extends Action {

    public ManageSubscriptionsAction() {
        setText("Manage Q Developer Pro Subscription");
    }

    @Override
    public final void run() {
        UiTelemetryProvider.emitClickEventMetric("manageSubscriptions");
        Activator.getLspProvider().getAmazonQServer()
            .thenAccept(server -> {
                ExecuteCommandParams params = new ExecuteCommandParams();
                params.setCommand("aws/chat/manageSubscription");
                server.getWorkspaceService().executeCommand(params).exceptionally(ex -> {
                    Activator.getLogger().error("Error executing manage subscriptions actions", ex);
                    return null;
                });
            });
    }

    public final void setVisible(final boolean isVisible) {
        super.setEnabled(isVisible);
    }
}
