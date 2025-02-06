// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import java.util.Optional;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import software.aws.toolkits.eclipse.amazonq.chat.ChatCommunicationManager;
import software.aws.toolkits.eclipse.amazonq.chat.ChatStateManager;
import software.aws.toolkits.eclipse.amazonq.chat.ChatTheme;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.AuthState;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspStatusManager;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.providers.assets.ChatWebViewAssetProvider;
import software.aws.toolkits.eclipse.amazonq.providers.assets.WebViewAssetProvider;
import software.aws.toolkits.eclipse.amazonq.util.ThreadingUtils;
import software.aws.toolkits.eclipse.amazonq.views.actions.AmazonQCommonActions;

public class AmazonQChatWebview extends AmazonQView implements ChatUiRequestListener {

    public static final String ID = "software.aws.toolkits.eclipse.amazonq.views.AmazonQChatWebview";

    private AmazonQCommonActions amazonQCommonActions;
    private final ChatStateManager chatStateManager;
    private final ViewCommandParser commandParser;
    private final ViewActionHandler actionHandler;
    private final ChatCommunicationManager chatCommunicationManager;
    private final ChatTheme chatTheme;
    private Browser browser;
    private volatile boolean canDisposeState = false;
    private WebViewAssetProvider webViewAssetProvider;

    public AmazonQChatWebview() {
        super();
        this.chatStateManager = ChatStateManager.getInstance();
        this.commandParser = new LoginViewCommandParser();
        this.chatCommunicationManager = ChatCommunicationManager.getInstance();
        this.actionHandler = new AmazonQChatViewActionHandler(chatCommunicationManager);
        this.webViewAssetProvider = new ChatWebViewAssetProvider();
        this.chatTheme = new ChatTheme();
    }

    @Override
    public final void createPartControl(final Composite parent) {
        setupParentBackground(parent);
        browser = chatStateManager.getBrowser(parent);
        // attempt to use existing browser with chat history if present, else create a
        // new one
        if (browser == null || browser.isDisposed()) {
            canDisposeState = false;
            var result = setupBrowser(parent);
            // if setup of amazon q view fails due to missing webview dependency, switch to
            // that view and don't setup rest of the content
            if (!result) {
                canDisposeState = true;
                showDependencyMissingView("update");
                return;
            }
            browser = getAndUpdateStateManager();

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
        } else {
            updateBrowser(browser);
        }

        AuthState authState = Activator.getLoginService().getAuthState();
        setupAmazonQView(parent, authState);

        parent.addDisposeListener(e -> chatStateManager.preserveBrowser());
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
                        disableBrowserContextMenu();
                    } catch (Exception e) {
                        Activator.getLogger().info("Error occurred while injecting theme into Q chat", e);
                    }
                });
            }
        });

        // Check if user is authenticated and build view accordingly
        onEvent(authState);
    }

    private Browser getAndUpdateStateManager() {
        var browser = getBrowser();
        chatStateManager.updateBrowser(browser);
        return browser;
    }

    @Override
    public final void onEvent(final AuthState authState) {
        Display.getDefault().asyncExec(() -> {
            amazonQCommonActions.updateActionVisibility(authState, getViewSite());
            if (authState.isExpired()) {
                canDisposeState = true;
                ViewVisibilityManager.showReAuthView("update");
            } else if (authState.isLoggedOut()) {
                canDisposeState = true;
                ViewVisibilityManager.showLoginView("update");
            } else {
                // if browser is not null and there is no chat prior state, start a new blank
                // chat view
                if (browser != null && !browser.isDisposed() && !chatStateManager.hasPreservedState()) {
                    Optional<String> content = webViewAssetProvider.getContent();
                    if (!content.isPresent() && !LspStatusManager.getInstance().lspFailed()) {
                        canDisposeState = true;
                        if (!LspStatusManager.getInstance().lspFailed()) {
                            ViewVisibilityManager.showChatAssetMissingView("update");
                        }
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
            Activator.getLogger().error("Error processing message from Amazon Q chat", e);
        }
    }

    @Override
    public final void onSendToChatUi(final String message) {
        String script = "window.postMessage(" + message + ");";
        browser.getDisplay().asyncExec(() -> {
            browser.evaluate(script);
        });
    }

    @Override
    public final void dispose() {
        chatCommunicationManager.removeListener();
        if (canDisposeState) {
            ChatStateManager.getInstance().dispose();
        }
        webViewAssetProvider.dispose();
        super.dispose();
    }
}
