// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;


import software.aws.toolkits.eclipse.amazonq.chat.ChatCommunicationManager;
import software.aws.toolkits.eclipse.amazonq.chat.ChatTheme;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginDetails;
import software.aws.toolkits.eclipse.amazonq.lsp.auth.model.LoginType;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.ChatContentProvider;
import software.aws.toolkits.eclipse.amazonq.util.ThreadingUtils;
import software.aws.toolkits.eclipse.amazonq.views.actions.AmazonQCommonActions;

public class AmazonQChatWebview extends AmazonQView implements ChatUiRequestListener {

    public static final String ID = "software.aws.toolkits.eclipse.amazonq.views.AmazonQChatWebview";

    private AmazonQCommonActions amazonQCommonActions;
    private ChatContentProvider chatContentProvider;

    private final ViewCommandParser commandParser;
    private final ViewActionHandler actionHandler;
    private ChatCommunicationManager chatCommunicationManager;
    private ChatTheme chatTheme;

    public AmazonQChatWebview() {
        super();
        this.commandParser = new LoginViewCommandParser();
        this.chatCommunicationManager = ChatCommunicationManager.getInstance();
        this.actionHandler = new AmazonQChatViewActionHandler(chatCommunicationManager);
        this.chatTheme = new ChatTheme();
        this.chatContentProvider = new ChatContentProvider();
    }

    @Override
    public final void createPartControl(final Composite parent) {
        LoginDetails loginInfo = new LoginDetails();
        loginInfo.setIsLoggedIn(false);
        loginInfo.setLoginType(LoginType.NONE);

        var result = setupAmazonQView(parent, loginInfo);
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
        Activator.getLoginService().getLoginDetails().thenAcceptAsync(loginDetails -> {
            onAuthStatusChanged(loginDetails);
        }, ThreadingUtils::executeAsyncTask);
    }

    @Override
    public final void onAuthStatusChanged(final LoginDetails loginDetails) {
        var browser = getBrowser();
        Display.getDefault().asyncExec(() -> {
            amazonQCommonActions.updateActionVisibility(loginDetails, getViewSite());
            if (!loginDetails.getIsLoggedIn()) {
                AmazonQView.showView(ReauthenticateView.ID);
            } else {
                if (!browser.isDisposed()) {
                    chatContentProvider.getContent().ifPresentOrElse(
                            content -> browser.setText(content),
                            this::showChatAssetMissingView
                        );
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
        chatContentProvider.dispose();
        chatCommunicationManager.removeListener();
        super.dispose();
    }

    private void showChatAssetMissingView() {
        AmazonQView.showView(ChatAssetMissingView.ID);
    }
}
