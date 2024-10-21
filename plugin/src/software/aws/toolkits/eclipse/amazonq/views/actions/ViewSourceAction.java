// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.actions;

import org.eclipse.jface.action.Action;

import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;

public final class ViewSourceAction extends Action {

    public ViewSourceAction() {
        setText("View Source on Github");
    }

    @Override
    public void run() {
        String link = "https://github.com/aws/amazon-q-eclipse";
        PluginUtils.handleExternalLinkClick(link);
    }
}
