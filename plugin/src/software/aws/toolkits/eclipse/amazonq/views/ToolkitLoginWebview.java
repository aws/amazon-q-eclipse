// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.telemetry.UiTelemetryProvider;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;
import software.aws.toolkits.eclipse.amazonq.util.ThemeDetector;
import software.aws.toolkits.eclipse.amazonq.util.WebviewAssetServer;
import software.aws.toolkits.eclipse.amazonq.views.actions.AmazonQCommonActions;

public final class ToolkitLoginWebview extends AmazonQView {

    public static final String ID = "software.aws.toolkits.eclipse.amazonq.views.ToolkitLoginWebview";

    private AmazonQCommonActions amazonQCommonActions;
    private WebviewAssetServer webviewAssetServer;
    private static final ThemeDetector THEME_DETECTOR = new ThemeDetector();

    private final ViewCommandParser commandParser;
    private final ViewActionHandler actionHandler;

    public ToolkitLoginWebview() {
        super();
        this.commandParser = new LoginViewCommandParser();
        this.actionHandler = new LoginViewActionHandler();
    }

    @Override
    public void createPartControl(final Composite parent) {
        setupParentBackground(parent);
        var result = setupBrowser(parent);
        // if setup of amazon q view fails due to missing webview dependency, switch to
        // that view
        // and don't setup rest of the content
        if (!result) {
            showDependencyMissingView("Failed to set up webview from Login");
            return;
        }
        var browser = getBrowser();
        browser.setVisible(false);
        browser.addProgressListener(new ProgressAdapter() {
            @Override
            public void completed(final ProgressEvent event) {
                Display.getDefault().asyncExec(() -> {
                    if (!browser.isDisposed()) {
                        browser.setVisible(true);
                    }
                });
            }
        });

        AuthState authState = Activator.getLoginService().getAuthState();
        setupAmazonQView(parent, authState);

        new BrowserFunction(browser, ViewConstants.COMMAND_FUNCTION_NAME) {
            @Override
            public Object function(final Object[] arguments) {
                commandParser.parseCommand(arguments)
                        .ifPresent(command -> actionHandler.handleCommand(command, browser));
                return null;
            }
        };
        new BrowserFunction(browser, "telemetryEvent") {
            @Override
            public Object function(final Object[] arguments) {
                String clickEvent = (String) arguments[0];
                UiTelemetryProvider.emitClickEventMetric("auth_" + clickEvent);
                return null;
            }
        };

        amazonQCommonActions = getAmazonQCommonActions();

        // Check if user is authenticated and build view accordingly
        onAuthStatusChanged(authState);
    }

    @Override
    public void onAuthStatusChanged(final AuthState authState) {
        var browser = getBrowser();
        Display.getDefault().asyncExec(() -> {
            amazonQCommonActions.updateActionVisibility(authState, getViewSite());
            if (!authState.isLoggedIn()) {
                if (!browser.isDisposed()) {
                    browser.setText(getContent());
                }
            } else {
                ViewVisibilityManager.showChatView("update");
            }
        });
    }

    private String getContent() {
        try {
            URL jsFile = PluginUtils.getResource("webview/build/assets/js/getStart.js");
            var jsParent = Path.of(jsFile.getPath()).getParent();
            var jsDirectoryPath = Path.of(jsParent.toUri()).normalize().toString();

            webviewAssetServer = new WebviewAssetServer();
            var result = webviewAssetServer.resolve(jsDirectoryPath);
            if (!result) {
                return "Failed to load JS";
            }
            var loginJsPath = webviewAssetServer.getUri() + "getStart.js";
            boolean isDarkTheme = THEME_DETECTOR.isDarkTheme();
            return String.format(
                    """
                            <!DOCTYPE html>
                            <html>
                                <head>
                                    <meta
                                        http-equiv="Content-Security-Policy"
                                        content="default-src 'none'; script-src %s 'unsafe-inline'; style-src %s 'unsafe-inline';
                                        img-src 'self' data:; object-src 'none'; base-uri 'none'; connect-src swt:;"
                                    >
                                    <title>AWS Q</title>
                                </head>
                                <body class="jb-light">
                                    <div id="app"></div>
                                    <script type="text/javascript" src="%s" defer></script>
                                    <script type="text/javascript">
                                        %s
                                        const init = () => {
                                            changeTheme(%b);
                                            Promise.all([
                                                waitForFunction('ideCommand'),
                                                waitForFunction('telemetryEvent')
                                            ])
                                                .then(([ideCommand, telemetryEvent]) => {
                                                    const ideApi = {
                                                        postMessage(message) {
                                                            ideCommand(JSON.stringify(message));
                                                        }
                                                    };
                                                    window.ideApi = ideApi;

                                                    const telemetryApi = {
                                                        postClickEvent(event) {
                                                            telemetryEvent(event);
                                                        }
                                                    };
                                                    window.telemetryApi = telemetryApi;

                                                    ideCommand(JSON.stringify({"command":"onLoad"}));
                                                })
                                                .catch(error => console.error('Error in initialization:', error));
                                        };
                                        window.addEventListener('load', init);
                                    </script>
                                </body>
                            </html>
                            """,
                    loginJsPath, loginJsPath, loginJsPath, getWaitFunction(), isDarkTheme);
        } catch (IOException e) {
            return "Failed to load JS";
        }
    }

    @Override
    public void dispose() {
        if (webviewAssetServer != null) {
            webviewAssetServer.stop();
        }
        var browser = getBrowser();
        if (browser != null && !browser.isDisposed()) {
            browser.dispose();
        }
        super.dispose();
    }
}
