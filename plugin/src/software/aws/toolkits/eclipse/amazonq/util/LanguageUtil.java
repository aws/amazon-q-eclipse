package software.aws.toolkits.eclipse.amazonq.util;

import java.util.Optional;
import java.util.Set;

public class LanguageUtil {
    private static final Set<String> defaultLanguages = Set.of(
            "yaml", "xsl", "xml", "vue", "tex", "typescript", "swift", "stylus",
            "sql", "slim", "shaderlab", "sass", "rust", "ruby", "r", "python",
            "pug", "powershell", "php", "perl", "markdown", "makefile", "lua",
            "less", "latex", "json", "javascript", "java", "ini", "html", "haml",
            "handlebars", "groovy", "go", "diff", "css", "c", "coffeescript",
            "clojure", "bibtex", "abap");

    public static String extractLanguageNameFromFileExtension(final String filePath) {
        if (filePath == null) {
            return null;
        }

        String extension = filePath.substring(filePath.lastIndexOf('.') + 1).toLowerCase();
        if (defaultLanguages.contains(extension)) {
            return extension;
        }

        return switch (extension) {
        case "bat" -> "bat";
        case "cpp" -> "c++";
        case "csharp" -> "c#";
        case "cuda-cpp" -> "c++";
        case "dockerfile" -> "dockerfile";
        case "fsharp" -> "f#";
        case "git-commit", "git-rebase" -> "git";
        case "javascriptreact" -> "javascript";
        case "jsonc" -> "json";
        case "objective-c" -> "objective-c";
        case "objective-cpp" -> "objective-c++";
        case "perl6" -> "raku";
        case "plaintext" -> null;
        case "jade" -> "pug";
        case "razor" -> "razor";
        case "scss" -> "sass";
        case "shellscript" -> "sh";
        case "typescriptreact" -> "typescript";
        case "vb" -> "visual-basic";
        case "vue-html" -> "vue";
        default -> {
            if (extension.contains("js") || extension.contains("node")) {
                yield "javascript";
            } else if (extension.contains("ts")) {
                yield "typescript";
            } else if (extension.contains("py")) {
                yield "python";
            }
            yield null;
        }
        };
    }

    public static String extractLanguageFromOpenFile() {
        Optional<String> uri = QEclipseEditorUtils.getOpenFileUri();
        if (uri.isPresent()) {
            String filePath = uri.get();
            String extension = filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase();
            return extractLanguageNameFromFileExtension(extension);
        } else {
            return null;
        }
    }
}
