// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views.actions;

import org.eclipse.jface.action.Action;

import software.aws.toolkits.eclipse.amazonq.preferences.AmazonQPreferencePage;

public class OpenPreferencesAction extends Action {

    public OpenPreferencesAction() {
        setText("Preferences");
    }

    @Override
    public final void run() {
        AmazonQPreferencePage.openPreferencePane();
    }

}
