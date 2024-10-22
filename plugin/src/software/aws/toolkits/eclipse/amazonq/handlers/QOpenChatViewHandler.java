// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;

import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;
import software.aws.toolkits.eclipse.amazonq.views.AmazonQChatWebview;

public class QOpenChatViewHandler extends AbstractHandler {
    @Override
    public final Object execute(final ExecutionEvent event) {
        PluginUtils.showView(AmazonQChatWebview.ID);
        return null;
    }
}
