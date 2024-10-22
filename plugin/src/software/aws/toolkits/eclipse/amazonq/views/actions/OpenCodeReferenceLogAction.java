// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.actions;

import org.eclipse.jface.action.Action;

import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;
import software.aws.toolkits.eclipse.amazonq.views.AmazonQCodeReferenceView;

public final class OpenCodeReferenceLogAction extends Action {

 public OpenCodeReferenceLogAction() {
     setText("Open Code Reference Log");
 }

 @Override
 public void run() {
     PluginUtils.showView(AmazonQCodeReferenceView.ID);
 }
}
