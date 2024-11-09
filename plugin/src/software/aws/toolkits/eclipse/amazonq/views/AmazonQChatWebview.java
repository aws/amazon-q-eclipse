// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import java.util.List;
import java.util.Optional;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.fasterxml.jackson.databind.ObjectMapper;

import software.aws.toolkits.eclipse.amazonq.chat.ChatCommunicationManager;
import software.aws.toolkits.eclipse.amazonq.chat.ChatTheme;
import software.aws.toolkits.eclipse.amazonq.lsp.AwsServerCapabiltiesProvider;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;
import software.aws.toolkits.eclipse.amazonq.lsp.model.ChatOptions;
import software.aws.toolkits.eclipse.amazonq.lsp.model.QuickActions;
import software.aws.toolkits.eclipse.amazonq.lsp.model.QuickActionsCommandGroup;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.ChatAssetProvider;
import software.aws.toolkits.eclipse.amazonq.util.ThreadingUtils;
import software.aws.toolkits.eclipse.amazonq.util.WebviewAssetServer;
import software.aws.toolkits.eclipse.amazonq.views.actions.AmazonQCommonActions;

public class AmazonQChatWebview extends AmazonQView implements ChatUiRequestListener {

    public static final String ID = "software.aws.toolkits.eclipse.amazonq.views.AmazonQChatWebview";

    private AmazonQCommonActions amazonQCommonActions;
    private WebviewAssetServer webviewAssetServer;

    private final ViewCommandParser commandParser;
    private final ViewActionHandler actionHandler;
    private ChatCommunicationManager chatCommunicationManager;
    private ChatTheme chatTheme;
    private ChatAssetProvider chatAssetProvider;

    public AmazonQChatWebview() {
        super();
        this.commandParser = new LoginViewCommandParser();
        this.chatCommunicationManager = ChatCommunicationManager.getInstance();
        this.actionHandler = new AmazonQChatViewActionHandler(chatCommunicationManager);
        this.chatTheme = new ChatTheme();
        this.chatAssetProvider = new ChatAssetProvider();
    }

    @Override
    public final void createPartControl(final Composite parent) {
        AuthState authState = Activator.getLoginService().getAuthState();

        var result = setupAmazonQView(parent, authState);
        // if setup of amazon q view fails due to missing webview dependency, switch to that view
        if (!result) {
            showDependencyMissingView();
            return;
        }
        var browser = getBrowser();
        amazonQCommonActions = getAmazonQCommonActions();
        chatCommunicationManager.setChatUiRequestListener(this);

        new BrowserFunction(browser, "ideCommand") {
            @Override
            public Object function(final Object[] arguments) {
                ThreadingUtils.executeAsyncTask(() -> {
                    handleMessageFromUI(browser, arguments);
                });
                return null;
            }
        };

        // Inject chat theme after mynah-ui has loaded
        browser.addProgressListener(new ProgressAdapter() {
            @Override
            public void completed(final ProgressEvent event) {
                Display.getDefault().syncExec(() -> {
                    try {
                        chatTheme.injectTheme(browser);
                    } catch (Exception e) {
                        Activator.getLogger().info("Error occurred while injecting theme", e);
                    }
                });
            }
        });

        // Check if user is authenticated and build view accordingly
        onAuthStatusChanged(authState);
    }

    @Override
    public final void onAuthStatusChanged(final AuthState authState) {
        var browser = getBrowser();
        Display.getDefault().asyncExec(() -> {
            amazonQCommonActions.updateActionVisibility(authState, getViewSite());
            if (authState.isExpired()) {
                AmazonQView.showView(ReauthenticateView.ID);
            } else if (authState.isLoggedOut()) {
                AmazonQView.showView(ToolkitLoginWebview.ID);
            } else {
                if (!browser.isDisposed()) {
                    Optional<String> content = getContent();
                    if (!content.isPresent()) {
                        AmazonQView.showView(ChatAssetMissingView.ID);
                    } else {
                        browser.setText(content.get()); // Display the chat client
                    }
                }
            }
        });
    }

    private void handleMessageFromUI(final Browser browser, final Object[] arguments) {
        try {
            commandParser.parseCommand(arguments)
                    .ifPresent(parsedCommand -> actionHandler.handleCommand(parsedCommand, browser));
        } catch (Exception e) {
            Activator.getLogger().error("Error processing message from Browser", e);
        }
    }

    private Optional<String> getContent() {
        var chatAsset = chatAssetProvider.get();

        if (!chatAsset.isPresent()) {
            return Optional.empty();
        }

        String chatJsPath = chatAsset.get();

        return Optional.of(String.format("""
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
                """, chatJsPath, chatJsPath, generateCss(), generateJS(chatJsPath)));
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
        var chatQuickActionConfig = generateQuickActionConfig();
        return String.format("""
                <script type="text/javascript" src="%s" defer onload="init()"></script>
                <script type="text/javascript">
                    const init = () => {
                        amazonQChat.createChat({
                           postMessage: (message) => {
                                ideCommand(JSON.stringify(message));
                           }
                        }, %s);
                    }
                </script>
                """, jsEntrypoint, chatQuickActionConfig);
    }

    /*
     * Generates javascript for chat options to be supplied to Chat UI defined here
     * https://github.com/aws/language-servers/blob/785f8dee86e9f716fcfa29b2e27eb07a02387557/chat-client/src/client/chat.ts#L87
     */
    private String generateQuickActionConfig() {
        return Optional.ofNullable(AwsServerCapabiltiesProvider.getInstance().getChatOptions())
                .map(ChatOptions::quickActions)
                .map(QuickActions::quickActionsCommandGroups)
                .map(this::serializeQuickActionCommands)
                .orElse("");
    }

    private String serializeQuickActionCommands(final List<QuickActionsCommandGroup> quickActionCommands) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(quickActionCommands);
            return String.format("{\"quickActionCommands\": %s}", json);
        } catch (Exception e) {
            Activator.getLogger().warn("Error occurred when json serializing quick action commands", e);
            return "";
        }
    }

    @Override
    public final void onSendToChatUi(final String message) {
        var browser = getBrowser();
        String script = "window.postMessage(" + message + ");";
        browser.getDisplay().asyncExec(() -> {
            browser.evaluate(script);
        });
    }

    @Override
    public final void dispose() {
        if (webviewAssetServer != null) {
            webviewAssetServer.stop();
        }
        chatCommunicationManager.removeListener();
        chatAssetProvider.dispose();
        super.dispose();
    }
}
