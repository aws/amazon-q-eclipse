// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.actions;

import org.eclipse.jface.action.Action;

import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;

public final class OpenUserGuideAction extends Action {

    public OpenUserGuideAction() {
        setText("Open User Guide");
    }

    @Override
    public void run() {
        String link = "https://docs.aws.amazon.com/amazonq/latest/qdeveloper-ug/q-in-IDE.html";
        PluginUtils.handleExternalLinkClick(link);
    }
}
