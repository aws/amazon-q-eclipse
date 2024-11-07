package software.aws.toolkits.eclipse.amazonq.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

import software.aws.toolkits.eclipse.amazonq.lsp.AwsServerCapabiltiesProvider;
import software.aws.toolkits.eclipse.amazonq.lsp.model.ChatOptions;
import software.aws.toolkits.eclipse.amazonq.lsp.model.QuickActions;
import software.aws.toolkits.eclipse.amazonq.lsp.model.QuickActionsCommandGroup;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.providers.LspManagerProvider;

public final class ChatContentProvider {
    private WebviewAssetServer webviewAssetServer;

    public Optional<String> getContent() {
        var chatUiDirectory = LspManagerProvider.getInstance().getLspInstallation().getClientDirectory();

        if (!isValid(chatUiDirectory)) {
            Activator.getLogger().error("Error loading Chat UI. If override used, please verify the override env variables else restart Eclipse");
            return Optional.empty();
        }

        String jsFile = Paths.get(chatUiDirectory).resolve("amazonq-ui.js").toString();
        var jsParent = Path.of(jsFile).getParent();
        var jsDirectoryPath = Path.of(jsParent.toUri()).normalize().toString();

        webviewAssetServer = new WebviewAssetServer();
        var result = webviewAssetServer.resolve(jsDirectoryPath);
        if (!result) {
            Activator.getLogger().error(String.format(
                    "Error loading Chat UI. Unable to find the `amazonq-ui.js` file in the directory: %s. Please verify and restart",
                    chatUiDirectory));
            return Optional.empty();
        }

        var chatJsPath = webviewAssetServer.getUri() + "amazonq-ui.js";
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

    private boolean isValid(final String chatUiDirectory) {
        return chatUiDirectory != null && !chatUiDirectory.isEmpty() && Files.exists(Paths.get(chatUiDirectory));
    }

    public void dispose() {
        if (webviewAssetServer != null) {
            webviewAssetServer.stop();
        }
    }
}
