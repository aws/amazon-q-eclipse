// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;


import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Display;

import software.aws.toolkits.eclipse.amazonq.util.AuthUtils;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;
import software.aws.toolkits.eclipse.amazonq.util.ThreadingUtils;
import software.aws.toolkits.eclipse.amazonq.views.model.ParsedCommand;

public class LoginViewActionHandler implements ViewActionHandler {
    @Override
    public final void handleCommand(final ParsedCommand parsedCommand, final Browser browser) {
        switch (parsedCommand.getCommand()) {
            case LOGIN_BUILDER_ID:
                PluginLogger.info("loginBuilderId command received");
                ThreadingUtils.executeAsyncTask(() -> {
                    try {
                        AuthUtils.signIn().get();
                        Display.getDefault().asyncExec(() -> {
                            browser.setText("Login succeeded");
                            AmazonQView.showView(AmazonQChatWebview.ID);
                        });
                    } catch (Exception e) {
                        PluginLogger.error("Failed to update token", e);
                    }
                });
                break;
            case CANCEL_LOGIN:
                PluginLogger.info("cancelLogin command received");
                break;
            default:
                System.out.println("Unknown command: " + parsedCommand.getCommand());
                break;
        }
    }
}
