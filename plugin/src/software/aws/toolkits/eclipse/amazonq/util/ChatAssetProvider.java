package software.aws.toolkits.eclipse.amazonq.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.providers.LspManagerProvider;

public final class ChatAssetProvider {
    private WebviewAssetServer webviewAssetServer;

    public Optional<String> get() {
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

        String chatJsPath = webviewAssetServer.getUri() + "amazonq-ui.js";

        return Optional.ofNullable(chatJsPath);
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