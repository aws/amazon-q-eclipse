// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.actions;

import org.eclipse.jface.action.Action;

import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;

public final class ReportAnIssueAction extends Action {

    public ReportAnIssueAction() {
        setText("Report an Issue");
    }

    @Override
    public void run() {
        String link = "https://github.com/aws/amazon-q-eclipse/issues";
        PluginUtils.handleExternalLinkClick(link);
    }
}
