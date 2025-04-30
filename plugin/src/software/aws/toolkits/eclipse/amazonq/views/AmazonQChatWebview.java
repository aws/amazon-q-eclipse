// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import io.reactivex.rxjava3.disposables.Disposable;
import software.aws.toolkits.eclipse.amazonq.broker.api.MissedReplayEventObserver;
import software.aws.toolkits.eclipse.amazonq.chat.ChatCommunicationManager;
import software.aws.toolkits.eclipse.amazonq.chat.ChatStateManager;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatUIInboundCommand;
import software.aws.toolkits.eclipse.amazonq.chat.models.ChatUIInboundCommandName;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.providers.assets.ChatWebViewAssetProvider;
import software.aws.toolkits.eclipse.amazonq.providers.assets.WebViewAssetProvider;
import software.aws.toolkits.eclipse.amazonq.util.JsonHandler;


public class AmazonQChatWebview extends AmazonQView implements MissedReplayEventObserver<ChatUIInboundCommand> {

    public static final String ID = "software.aws.toolkits.eclipse.amazonq.views.AmazonQChatWebview";

    private final ChatStateManager chatStateManager;
    private final ChatCommunicationManager chatCommunicationManager;
    private Browser browser;
    private volatile boolean canDisposeState = false;
    private WebViewAssetProvider webViewAssetProvider;
    private JsonHandler jsonHandler = new JsonHandler();
    private Disposable chatUIInboundCommandsSubscription;

    public AmazonQChatWebview() {
        super();
        chatStateManager = ChatStateManager.getInstance();
        chatCommunicationManager = ChatCommunicationManager.getInstance();
        webViewAssetProvider = new ChatWebViewAssetProvider();
        webViewAssetProvider.initialize();
    }

    @Override
    public final Composite setupView(final Composite parent) {
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
                return parent;
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

            webViewAssetProvider.injectAssets(browser);
        } else {
            updateBrowser(browser);
        }

        super.setupView(parent);

        parent.addDisposeListener(e -> chatStateManager.preserveBrowser());

        addFocusListener(parent, browser);
        setupAmazonQCommonActions();

        chatUIInboundCommandsSubscription = Activator.getEventBroker().subscribe(ChatUIInboundCommand.class, this);
        return parent;
    }

    private Browser getAndUpdateStateManager() {
        var browser = getBrowser();
        chatStateManager.updateBrowser(browser);
        return browser;
    }

    @Override
    public final void onEvent(final ChatUIInboundCommand command) {
        String message = jsonHandler.serialize(command);
        String inlineChatCommand = ChatUIInboundCommandName.InlineChatPrompt.getValue();
        if (!inlineChatCommand.equals(command.command())) {
            String script = "window.postMessage(" + message + ");";
            browser.getDisplay().asyncExec(() -> {
                browser.evaluate(script);
            });
        }
    }

    @Override
    public final String getSubscribingComponentId() {
        return ID;
    }

    public final void disposeBrowserState() {
        canDisposeState = true;
    }

    @Override
    public final void dispose() {
        if (chatUIInboundCommandsSubscription != null && !chatUIInboundCommandsSubscription.isDisposed()) {
            chatUIInboundCommandsSubscription.dispose();
            chatUIInboundCommandsSubscription = null;
        }
        if (canDisposeState) {
            ChatStateManager.getInstance().dispose();
        }
        super.dispose();
    }
}
