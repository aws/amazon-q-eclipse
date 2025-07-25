// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.lsp4j.ExecuteCommandParams;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.telemetry.UiTelemetryProvider;

import java.util.Collections;

/**
 * Action to show account details and subscription information.
 * Based on VS and JetBrains reference implementations.
 */
public final class AccountDetailsAction extends Action {

    public AccountDetailsAction() {
        setText("Account Details");
        setToolTipText("View account details and subscription information");
    }

    @Override
    public void run() {
        UiTelemetryProvider.emitClickEventMetric("accountDetails");

        System.out.println("[DEBUG] AccountDetailsAction.run() called");

        Activator.getLspProvider().getAmazonQServer()
            .thenAccept(server -> {
                System.out.println("[DEBUG] Got Amazon Q server: " + server.getClass().getSimpleName());

                ExecuteCommandParams params = new ExecuteCommandParams();
                params.setCommand("aws/chat/subscription/show");
                params.setArguments(Collections.emptyList());

                System.out.println("[DEBUG] Executing command: " + params.getCommand());

                server.getWorkspaceService().executeCommand(params)
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            System.err.println("[DEBUG] Command execution failed: " + throwable.getMessage());
                            throwable.printStackTrace();
                            Activator.getLogger().error("Failed to execute subscription/show command", throwable);
                        } else {
                            System.out.println("[DEBUG] Command executed successfully, result: " + result);
                            Activator.getLogger().info("Successfully executed subscription/show command");
                        }
                    });
            })
            .exceptionally(ex -> {
                System.err.println("[DEBUG] Failed to get Amazon Q server: " + ex.getMessage());
                ex.printStackTrace();
                Activator.getLogger().error("Failed to get Amazon Q server for subscription details", ex);
                return null;
            });
    }

    public void setVisible(final boolean isVisible) {
        super.setEnabled(isVisible);
    }
}
