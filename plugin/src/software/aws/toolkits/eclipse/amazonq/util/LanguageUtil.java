package software.aws.toolkits.eclipse.amazonq.util;

import java.util.Set;

public final class LanguageUtil {
    private LanguageUtil() {
        // Prevent instantiation
    }
    private static final Set<String> DEFAULT_LANGUAGES = Set.of("yaml", "xsl",
            "xml", "vue", "tex", "typescript", "swift", "stylus", "sql", "slim",
            "shaderlab", "sass", "rust", "ruby", "r", "python", "pug",
            "powershell", "php", "perl", "markdown", "makefile", "lua", "less",
            "latex", "json", "javascript", "java", "ini", "html", "haml",
            "handlebars", "groovy", "go", "diff", "css", "c", "coffeescript",
            "clojure", "bibtex", "abap");

    public static String extractLanguageNameFromFileExtension(
            final String languageId) {

        if (languageId == null) {
            return null;
        }

        if (DEFAULT_LANGUAGES.contains(languageId)) {
            return languageId;
        }

        switch (languageId) {
            case "bat" :
                return "bat";
            case "cpp" :
                return "c++";
            case "csharp" :
                return "c#";
            case "cuda-cpp" :
                return "c++";
            case "dockerfile" :
                return "dockerfile";
            case "fsharp" :
                return "f#";
            case "git-commit" :
                return "git";
            case "git-rebase" :
                return "git";
            case "javascriptreact" :
                return "javascript";
            case "jsonc" :
                return "json";
            case "objective-c" :
                return "objective-c";
            case "objective-cpp" :
                return "objective-c++";
            case "perl6" :
                return "raku";
            case "plaintext" :
                return null;
            case "jade" :
                return "pug";
            case "razor" :
                return "razor";
            case "scss" :
                return "sass";
            case "shellscript" :
                return "sh";
            case "typescriptreact" :
                return "typescript";
            case "vb" :
                return "visual-basic";
            case "vue-html" :
                return "vue";
            default :
                if (languageId.contains("javascript")
                        || languageId.contains("node")) {
                    return "javascript";
                } else if (languageId.contains("typescript")) {
                    return "typescript";
                } else if (languageId.contains("python")) {
                    return "python";
                }
                return null;
        }
    }

    public static String extractLanguageFromFileUri(final String fileUri) {
        int lastDotIdx = fileUri.lastIndexOf('.');
        String fileExt = lastDotIdx > 0
                ? fileUri.substring(lastDotIdx + 1)
                : null;
        return extractLanguageNameFromFileExtension(fileExt);
    }
}
