// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

import software.aws.toolkits.eclipse.amazonq.views.ViewVisibilityManager;

public class QOpenLoginViewHandler extends AbstractHandler {
    @Override
    public final Object execute(final ExecutionEvent event) {
        if (Activator.getLoginService().getAuthState().isLoggedIn()) {
            ViewVisibilityManager.showChatView("statusBar");
        } else {
            ViewVisibilityManager.showLoginView("statusBar");
        }
        return null;
    }
}
