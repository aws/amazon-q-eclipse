// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import java.nio.file.Path;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspConstants;
import software.aws.toolkits.eclipse.amazonq.util.AuthUtils;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;
import software.aws.toolkits.eclipse.amazonq.util.ThreadingUtils;
import software.aws.toolkits.eclipse.amazonq.util.WebviewAssetServer;
import software.aws.toolkits.eclipse.amazonq.views.actions.AmazonQCommonActions;

public class AmazonQChatWebview extends AmazonQView {

    public static final String ID = "software.aws.toolkits.eclipse.amazonq.views.AmazonQChatWebview";

    private AmazonQCommonActions amazonQCommonActions;
    private WebviewAssetServer webviewAssetServer;

    private final ViewCommandParser commandParser;
    private final ViewActionHandler actionHandler;

    public AmazonQChatWebview() {
        this.commandParser = new LoginViewCommandParser();
        this.actionHandler = new AmazonQChatViewActionHandler();
    }

    @Override
    public final void createPartControl(final Composite parent) {
        setupAmazonQView(parent, true);
        var browser = getBrowser();
        amazonQCommonActions = getAmazonQCommonActions();

        AuthUtils.isLoggedIn().thenAcceptAsync(isLoggedIn -> {
            handleAuthStatusChange(isLoggedIn);
        }, ThreadingUtils::executeAsyncTask);

       new BrowserFunction(browser, "ideCommand") {
            @Override
            public Object function(final Object[] arguments) {
                ThreadingUtils.executeAsyncTask(() -> {
                    try {
                        commandParser.parseCommand(arguments)
                            .ifPresent(parsedCommand -> actionHandler.handleCommand(parsedCommand, browser));
                    } catch (Exception e) {
                        PluginLogger.error("Error processing message from Browser", e);
                    }
                });
                return null;
            }
        };
    }

    private String getContent() {
        String jsFile = PluginUtils.getAwsDirectory(LspConstants.LSP_SUBDIRECTORY).resolve("amazonq-ui.js").toString();
        var jsParent = Path.of(jsFile).getParent();
        var jsDirectoryPath = Path.of(jsParent.toUri()).normalize().toString();

        webviewAssetServer = new WebviewAssetServer();
        var result = webviewAssetServer.resolve(jsDirectoryPath);
        if (!result) {
            return "Failed to load JS";
        }

        var chatJsPath = webviewAssetServer.getUri() + "amazonq-ui.js";
        return String.format("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <meta
                        http-equiv="Content-Security-Policy"
                        content="default-src 'none'; script-src %s 'unsafe-inline'; style-src %s 'unsafe-inline';
                        img-src 'self' data:; object-src 'none'; base-uri 'none';"
                    >
                    <title>Chat UI</title>
                    %s
                </head>
                <body>
                    %s
                </body>
                </html>
                """, chatJsPath, chatJsPath, generateCss(), generateJS(chatJsPath));
    }

    private String generateCss() {
        return """
                <style>
                    body,
                    html {
                        background-color: var(--mynah-color-bg);
                        color: var(--mynah-color-text-default);
                        height: 100vh;
                        width: 100%%;
                        overflow: hidden;
                        margin: 0;
                        padding: 0;
                    }
                    textarea:placeholder-shown {
                        line-height: 1.5rem;
                    }
                </style>
                """;
    }

    private String generateJS(final String jsEntrypoint) {
        return String.format("""
                <script type="text/javascript" src="%s" defer onload="init()"></script>
                <script type="text/javascript">
                    const init = () => {
                        amazonQChat.createChat({
                           postMessage: (message) => {
                                ideCommand(JSON.stringify(message));
                           }
                        });
                    }
                </script>
                """, jsEntrypoint);
    }

    @Override
    protected final void handleAuthStatusChange(final boolean isLoggedIn) {
        var browser = getBrowser();
        Display.getDefault().asyncExec(() -> {
            amazonQCommonActions.updateActionVisibility(isLoggedIn, getViewSite());
            if (!isLoggedIn) {
                browser.setText("Signed Out");
                AmazonQView.showView(ToolkitLoginWebview.ID);
            } else {
                browser.setText(getContent());
            }
        });
    }

    @Override
    public final void dispose() {
        if (webviewAssetServer != null) {
            webviewAssetServer.stop();
        }
        super.dispose();
    }
}
