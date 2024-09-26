// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import java.nio.file.Path;

import org.eclipse.jetty.server.Server;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspConstants;
import software.aws.toolkits.eclipse.amazonq.util.AuthUtils;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;
import software.aws.toolkits.eclipse.amazonq.util.ThreadingUtils;
import software.aws.toolkits.eclipse.amazonq.views.actions.AmazonQCommonActions;

public class AmazonQChatWebview extends AmazonQView {

    public static final String ID = "software.aws.toolkits.eclipse.amazonq.views.AmazonQChatWebview";

    private AmazonQCommonActions amazonQCommonActions;
    private Server server;

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
       
        server = setupVirtualServer(jsDirectoryPath);
        if(server == null) {
        	return "Failed to load JS";
        }
        
        var chatJsPath = server.getURI().toString()+"amazonq-ui.js";
        return String.format("<!DOCTYPE html>\n"
                + "<html lang=\"en\">\n"
                + "<head>\n"
                + "    <meta charset=\"UTF-8\">\n"
                + "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
                + "    <meta \n"
                + "          http-equiv=\"\"Content-Security-Policy\"\" \n"
                + "          content=\"\"default-src 'none'; script-src %s 'unsafe-inline'; style-src {javascriptFilePath} 'unsafe-inline'; img-src 'self' data:; object-src 'none'; base-uri 'none'; upgrade-insecure-requests;\"\"\n"
                + "        >"
                + "    <title>Chat UI</title>\n"
                + "    %s\n"
                + "</head>\n"
                + "<body>\n"
                + "    %s\n"
                + "</body>\n"
                + "</html>", chatJsPath, generateCss(), generateJS(chatJsPath));
    }

    private String generateCss() {
        return "<style>\n"
                + "        body,\n"
                + "        html {\n"
                + "            background-color: var(--mynah-color-bg);\n"
                + "            color: var(--mynah-color-text-default);\n"
                + "            height: 100vh;\n"
                + "            width: 100%%;\n"
                + "            overflow: hidden;\n"
                + "            margin: 0;\n"
                + "            padding: 0;\n"
                + "        }\n"
                + "        textarea:placeholder-shown {\n"
                + "            line-height: 1.5rem;\n"
                + "        }"
                + "    </style>";
    }

    private String generateJS(final String jsEntrypoint) {
        return String.format("<script type=\"text/javascript\" src=\"%s\" defer onload=\"init()\"></script>\n"
                + "    <script type=\"text/javascript\">\n"
                + "        const init = () => {\n"
                + "            amazonQChat.createChat({\n"
                + "               postMessage: (message) => {\n"
                + "                    ideCommand(JSON.stringify(message));\n"
                + "               }\n"
                + "         });\n"
                + "        }\n"
                + "    </script>", jsEntrypoint);
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
    public void dispose() {
        stopVirtualServer(server);
        super.dispose();
    }

}
