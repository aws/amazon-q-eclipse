// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginDetails;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;
import software.aws.toolkits.eclipse.amazonq.util.ThreadingUtils;
import software.aws.toolkits.eclipse.amazonq.util.WebviewAssetServer;
import software.aws.toolkits.eclipse.amazonq.views.actions.AmazonQCommonActions;

public final class ToolkitLoginWebview extends AmazonQView {

    public static final String ID = "software.aws.toolkits.eclipse.amazonq.views.ToolkitLoginWebview";

    private AmazonQCommonActions amazonQCommonActions;
    private WebviewAssetServer webviewAssetServer;

    private final ViewCommandParser commandParser;
    private final ViewActionHandler actionHandler;

    public ToolkitLoginWebview() {
        super();
        this.commandParser = new LoginViewCommandParser();
        this.actionHandler = new LoginViewActionHandler();
    }

    @Override
    public void createPartControl(final Composite parent) {
        LoginDetails loginInfo = new LoginDetails();
        loginInfo.setIsLoggedIn(true);
        loginInfo.setLoginType(LoginType.BUILDER_ID);
        var result = setupAmazonQView(parent, loginInfo);
        // if setup of amazon q view fails due to missing webview dependency, switch to that view
        if (!result) {
            showDependencyMissingView();
            return;
        }
        var browser = getBrowser();
        amazonQCommonActions = getAmazonQCommonActions();

        Activator.getLoginService().getLoginDetails().thenAcceptAsync(loginDetails -> {
            handleAuthStatusChange(loginDetails);
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

    protected void handleAuthStatusChange(final LoginDetails loginDetails) {
        var browser = getBrowser();
        Display.getDefault().asyncExec(() -> {
            amazonQCommonActions.updateActionVisibility(loginDetails, getViewSite());
            if (!loginDetails.getIsLoggedIn()) {
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
            var jsParent = Path.of(jsFile.toURI()).getParent();
            var jsDirectoryPath = Path.of(jsParent.toUri()).normalize().toString();

            webviewAssetServer = new WebviewAssetServer();
            var result = webviewAssetServer.resolve(jsDirectoryPath);
            if (!result) {
                return "Failed to load JS";
            }
            var loginJsPath = webviewAssetServer.getUri() + "getStart.js";

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
                                window.onload = function() {
                                    ideCommand(JSON.stringify({"command":"onLoad"}));
                                }
                            </script>
                        </body>
                    </html>
                    """, loginJsPath);
        } catch (IOException | URISyntaxException e) {
            return "Failed to load JS";
        }
    }

    @Override
    public void dispose() {
        if (webviewAssetServer != null) {
            webviewAssetServer.stop();
        }
        super.dispose();
    }
}
