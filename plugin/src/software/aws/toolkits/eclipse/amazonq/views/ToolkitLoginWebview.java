// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import java.io.IOException;
import java.net.URL;

import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import software.aws.toolkits.eclipse.amazonq.util.AuthUtils;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;
import software.aws.toolkits.eclipse.amazonq.util.ThreadingUtils;
import software.aws.toolkits.eclipse.amazonq.views.actions.AmazonQCommonActions;

public final class ToolkitLoginWebview extends AmazonQView {

    public static final String ID = "software.aws.toolkits.eclipse.amazonq.views.ToolkitLoginWebview";

    private AmazonQCommonActions amazonQCommonActions;

    private final ViewCommandParser commandParser;
    private final ViewActionHandler actionHandler;

    public ToolkitLoginWebview() {
        this.commandParser = new LoginViewCommandParser();
        this.actionHandler = new LoginViewActionHandler();
    }

    @Override
    public void createPartControl(final Composite parent) {
        setupAmazonQView(parent, true);
        var browser = getBrowser();
        amazonQCommonActions = getAmazonQCommonActions();

        AuthUtils.isLoggedIn().thenAcceptAsync(isLoggedIn -> {
            handleAuthStatusChange(isLoggedIn);
        }, ThreadingUtils::executeAsyncTask);

        new BrowserFunction(browser, ViewConstants.COMMAND_FUNCTION_NAME) {
            @Override
            public Object function(final Object[] arguments) {
                commandParser.parseCommand(arguments)
                        .ifPresent(command -> actionHandler.handleCommand(command, browser));
                return null;
            }
        };
    }

    protected void handleAuthStatusChange(final boolean isLoggedIn) {
        var browser = getBrowser();
        Display.getDefault().asyncExec(() -> {
            amazonQCommonActions.updateActionVisibility(isLoggedIn, getViewSite());
            if (!isLoggedIn) {
                browser.setText(getContent());
            } else {
                browser.setText("Signed in");
                AmazonQView.showView(AmazonQChatWebview.ID);
            }
        });
    }

    private String getContent() {
        try {
            URL jsFile = PluginUtils.getResource("webview/build/assets/js/getStart.js");
            return String.format("""
                    <!DOCTYPE html>
                    <html>
                        <head>
                            <title>AWS Q</title>
                        </head>
                        <body class="jb-light">
                            <div id="app"></div>
                            <script type="text/javascript" src="%s"></script>
                            <script>
                                window.addEventListener('DOMContentLoaded', function() {
                                    const ideApi = {
                                        postMessage(message) {
                                            ideCommand(JSON.stringify(message));
                                        }
                                    };
                                    window.ideApi = ideApi;
                                });
                            </script>
                        </body>
                    </html>
                    """, jsFile.toString());
        } catch (IOException e) {
            return "Failed to load JS";
        }
    }

}
