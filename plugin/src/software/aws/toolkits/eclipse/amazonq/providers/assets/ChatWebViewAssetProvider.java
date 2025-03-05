// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.providers.assets;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.widgets.Display;

import com.fasterxml.jackson.databind.ObjectMapper;

import software.aws.toolkits.eclipse.amazonq.broker.events.ChatWebViewAssetState;
import software.aws.toolkits.eclipse.amazonq.chat.ChatCommunicationManager;
import software.aws.toolkits.eclipse.amazonq.chat.ChatTheme;
import software.aws.toolkits.eclipse.amazonq.configuration.PluginStoreKeys;
import software.aws.toolkits.eclipse.amazonq.lsp.AwsServerCapabiltiesProvider;
import software.aws.toolkits.eclipse.amazonq.lsp.model.ChatOptions;
import software.aws.toolkits.eclipse.amazonq.lsp.model.QuickActions;
import software.aws.toolkits.eclipse.amazonq.lsp.model.QuickActionsCommandGroup;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.providers.lsp.LspManagerProvider;
import software.aws.toolkits.eclipse.amazonq.util.ObjectMapperFactory;
import software.aws.toolkits.eclipse.amazonq.util.PluginPlatform;
import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;
import software.aws.toolkits.eclipse.amazonq.util.ThreadingUtils;
import software.aws.toolkits.eclipse.amazonq.util.WebviewAssetServer;
import software.aws.toolkits.eclipse.amazonq.views.AmazonQChatViewActionHandler;
import software.aws.toolkits.eclipse.amazonq.views.LoginViewCommandParser;
import software.aws.toolkits.eclipse.amazonq.views.ViewActionHandler;
import software.aws.toolkits.eclipse.amazonq.views.ViewCommandParser;

public final class ChatWebViewAssetProvider extends WebViewAssetProvider {

    private WebviewAssetServer webviewAssetServer;
    private final ChatTheme chatTheme;
    private final ViewCommandParser commandParser;
    private final ViewActionHandler actionHandler;
    private final ChatCommunicationManager chatCommunicationManager;
    private Optional<String> content;

    public ChatWebViewAssetProvider() {
        chatTheme = new ChatTheme();
        commandParser = new LoginViewCommandParser();
        chatCommunicationManager = ChatCommunicationManager.getInstance();
        actionHandler = new AmazonQChatViewActionHandler(chatCommunicationManager);
        content = Optional.empty();
    }

    @Override
    public void initialize() {
        if (content.isEmpty()) {
            content = resolveContent();
            Activator.getEventBroker().post(ChatWebViewAssetState.class,
                    content.isPresent() ? ChatWebViewAssetState.RESOLVED : ChatWebViewAssetState.DEPENDENCY_MISSING);
        }
    }

    @Override
    public void injectAssets(final Browser browser) {
        new BrowserFunction(browser, "ideCommand") {
            @Override
            public Object function(final Object[] arguments) {
                ThreadingUtils.executeAsyncTask(() -> {
                    handleMessageFromUI(browser, arguments);
                });
                return null;
            }
        };

        new BrowserFunction(browser, "isMacOs") {
            @Override
            public Object function(final Object[] arguments) {
                return Boolean.TRUE.equals(PluginUtils.getPlatform() == PluginPlatform.MAC);
            }
        };

        new BrowserFunction(browser, "copyToClipboard") {
            @Override
            public Object function(final Object[] arguments) {
                if (arguments.length > 0 && arguments[0] instanceof String) {
                    StringSelection stringSelection = new StringSelection((String) arguments[0]);
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
                }
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
                        disableBrowserContextMenu(browser);
                    } catch (Exception e) {
                        Activator.getLogger().info("Error occurred while injecting theme into Q chat", e);
                    }
                });
            }
        });

        browser.setText(content.get());
    }

    private Optional<String> resolveContent() {
        var chatAsset = resolveJsPath();
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
                        img-src 'self' data:; object-src 'none'; base-uri 'none'; connect-src swt:;"
                    >
                    <title>Amazon Q Chat</title>
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
                    .mynah-ui-icon-plus,
                    .mynah-ui-icon-cancel {
                        -webkit-mask-size: 155% !important;
                        mask-size: 155% !important;
                        mask-position: center;
                        scale: 60%;
                    }
                    .code-snippet-close-button i.mynah-ui-icon-cancel,
                    .mynah-chat-item-card-related-content-show-more i.mynah-ui-icon-down-open {
                        -webkit-mask-size: 195.5% !important;
                        mask-size: 195.5% !important;
                        mask-position: center;
                        aspect-ratio: 1/1;
                        width: 15px;
                        height: 15px;
                        scale: 50%
                    }
                    .mynah-ui-icon-tabs {
                        -webkit-mask-size: 102% !important;
                        mask-size: 102% !important;
                        mask-position: center;
                    }
                    textarea:placeholder-shown {
                        line-height: 1.5rem;
                    }
                    .mynah-ui-spinner-container > span.mynah-ui-spinner-logo-part > .mynah-ui-spinner-logo-mask.text {
                        opacity: 1 !important;
                    }
                </style>
                """;
    }

    private String generateJS(final String jsEntrypoint) {
        var chatQuickActionConfig = generateQuickActionConfig();
        var disclaimerAcknowledged = Activator.getPluginStore().get(PluginStoreKeys.CHAT_DISCLAIMER_ACKNOWLEDGED);
        return String.format("""
                <script type="text/javascript" src="%s" defer></script>
                <script type="text/javascript">
                    %s
                    const init = () => {
                        waitForFunction('ideCommand')
                            .then(() => {
                                amazonQChat.createChat({
                                    postMessage: (message) => {
                                        ideCommand(JSON.stringify(message));
                                    }
                                },
                                {
                                    quickActionCommands: %s,
                                    disclaimerAcknowledged: %b
                                });
                            })
                            .catch(error => console.error('Error initializing chat:', error));
                    }

                    window.addEventListener('load', init);

                    %s

                    %s

                    %s

                    %s

                </script>
                """, jsEntrypoint, getWaitFunction(), chatQuickActionConfig, "true".equals(disclaimerAcknowledged),
                getArrowKeyBlockingFunction(), getSelectAllAndCopySupportFunctions(), getPreventEmptyPopupFunction(),
                getFocusOnChatPromptFunction());
    }

    private String getArrowKeyBlockingFunction() {
        return """
                window.addEventListener('load', () => {
                    const textarea = document.querySelector('textarea.mynah-chat-prompt-input');
                    if (textarea) {
                        textarea.addEventListener('keydown', (event) => {
                            const cursorPosition = textarea.selectionStart;
                            const hasText = textarea.value.length > 0;

                            // block arrow keys on empty text area
                            switch (event.key) {
                                case 'ArrowLeft':
                                    if (!hasText || cursorPosition === 0) {
                                        event.preventDefault();
                                        event.stopPropagation();
                                    }
                                    break;

                                case 'ArrowRight':
                                    if (!hasText || cursorPosition === textarea.value.length) {
                                        event.preventDefault();
                                        event.stopPropagation();
                                    }
                                    break;
                            }
                        });
                    }
                });
                """;
    }

    private String getSelectAllAndCopySupportFunctions() {
        return """
                window.addEventListener('load', () => {
                    const textarea = document.querySelector('textarea.mynah-chat-prompt-input');
                    if (textarea) {
                        textarea.addEventListener("keydown", (event) => {
                            if (((isMacOs() && event.metaKey) || (!isMacOs() && event.ctrlKey))
                                    && event.key === 'a') {
                                textarea.select();
                                event.preventDefault();
                                event.stopPropagation();
                            }
                        });
                    }
                });

                window.addEventListener('load', () => {
                    const textarea = document.querySelector('textarea.mynah-chat-prompt-input');
                    if (textarea) {
                        textarea.addEventListener("keydown", (event) => {
                            if (((isMacOs() && event.metaKey) || (!isMacOs() && event.ctrlKey))
                                    && event.key === 'c') {
                                copyToClipboard(textarea.value);
                                event.preventDefault();
                                event.stopPropagation();
                            }
                        });
                    }
                });
                """;
    }

    private String getPreventEmptyPopupFunction() {
        String selector = ".mynah-button" + ".mynah-button-secondary.mynah-button-border" + ".fill-state-always"
                + ".mynah-chat-item-followup-question-option" + ".mynah-ui-clickable-item";

        return """
                const observer = new MutationObserver((mutations) => {
                    try {
                        const selector = '%s';

                        mutations.forEach((mutation) => {
                            mutation.addedNodes.forEach((node) => {
                                if (node.nodeType === 1) { // Check if it's an element node
                                    // Check for direct match
                                    if (node.matches && node.matches(selector)) {
                                        attachEventListeners(node);
                                    }
                                    // Check for nested matches
                                    if (node.querySelectorAll) {
                                        const buttons = node.querySelectorAll(selector); // Missing selector parameter
                                        buttons.forEach(attachEventListeners);
                        }
                    }
                            });
                        });
                    } catch (error) {
                        console.error('Error in mutation observer:', error);
                    }
                });

                function attachEventListeners(element) {
                    if (!element || element.dataset.hasListener) return; // Prevent duplicate listeners

                    const handleMouseOver = function(event) {
                        const textSpan = this.querySelector('span.mynah-button-label');
                        if (textSpan && textSpan.scrollWidth <= textSpan.offsetWidth) {
                            event.stopImmediatePropagation();
                            event.stopPropagation();
                            event.preventDefault();
                        }
                    };

                    element.addEventListener('mouseover', handleMouseOver, true);
                    element.dataset.hasListener = 'true';
                }

                observer.observe(document.body, {
                    childList: true,
                    subtree: true,
                    attributes: true
                });
                """.formatted(selector);
    }

    private String getFocusOnChatPromptFunction() {
        return """
                window.addEventListener('load', () => {
                    const chatContainer = document.querySelector('.mynah-chat-prompt');
                    if (chatContainer) {
                        chatContainer.addEventListener('click', (event) => {
                            if (!event.target.closest('.mynah-chat-prompt-input')) {
                                keepFocusOnPrompt();
                            }
                        });
                    }
                });
                """;
    }

    /*
     * Generates javascript for chat options to be supplied to Chat UI defined here
     * https://github.com/aws/language-servers/blob/
     * 785f8dee86e9f716fcfa29b2e27eb07a02387557/chat-client/src/client/chat.ts#L87
     */
    private String generateQuickActionConfig() {
        return Optional.ofNullable(AwsServerCapabiltiesProvider.getInstance().getChatOptions())
                .map(ChatOptions::quickActions).map(QuickActions::quickActionsCommandGroups)
                .map(this::serializeQuickActionCommands).orElse("[]");
    }

    private String serializeQuickActionCommands(final List<QuickActionsCommandGroup> quickActionCommands) {
        try {
            ObjectMapper mapper = ObjectMapperFactory.getInstance();
            return mapper.writeValueAsString(quickActionCommands);
        } catch (Exception e) {
            Activator.getLogger().warn("Error occurred when json serializing quick action commands", e);
            return "";
        }
    }

    private void handleMessageFromUI(final Browser browser, final Object[] arguments) {
        try {
            commandParser.parseCommand(arguments)
                    .ifPresent(parsedCommand -> actionHandler.handleCommand(parsedCommand, browser));
        } catch (Exception e) {
            Activator.getLogger().error("Error processing message from Amazon Q chat", e);
        }
    }

    public Optional<String> resolveJsPath() {
        var chatUiDirectory = getChatUiDirectory();

        if (!isValid(chatUiDirectory)) {
            Activator.getLogger().error(
                    "Error loading Chat UI. If override used, please verify the override env variables else restart Eclipse");
            return Optional.empty();
        }

        String jsFile = Paths.get(chatUiDirectory.get()).resolve("amazonq-ui.js").toString();
        var jsParent = Path.of(jsFile).getParent();
        var jsDirectoryPath = Path.of(jsParent.toUri()).normalize().toString();

        if (webviewAssetServer == null) {
            webviewAssetServer = new WebviewAssetServer();
        }

        var result = webviewAssetServer.resolve(jsDirectoryPath);
        if (!result) {
            Activator.getLogger().error(String.format(
                    "Error loading Chat UI. Unable to find the `amazonq-ui.js` file in the directory: %s. Please verify and restart",
                    chatUiDirectory.get()));
            return Optional.empty();
        }

        String chatJsPath = webviewAssetServer.getUri() + "amazonq-ui.js";

        return Optional.ofNullable(chatJsPath);
    }

    private Optional<String> getChatUiDirectory() {
        try {
            return Optional.of(LspManagerProvider.getInstance().getLspInstallation().getClientDirectory());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private boolean isValid(final Optional<String> chatUiDirectory) {
        return chatUiDirectory.isPresent() && Files.exists(Paths.get(chatUiDirectory.get()));
    }

    @Override
    public void dispose() {
        if (webviewAssetServer != null) {
            webviewAssetServer.stop();
        }
        webviewAssetServer = null;
    }

}
