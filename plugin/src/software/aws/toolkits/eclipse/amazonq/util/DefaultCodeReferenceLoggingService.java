package software.aws.toolkits.eclipse.amazonq.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import software.aws.toolkits.eclipse.amazonq.chat.models.ReferenceTrackerInformation;
import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionReference;
import software.aws.toolkits.eclipse.amazonq.views.model.ChatCodeReference;
import software.aws.toolkits.eclipse.amazonq.views.model.CodeReferenceLogItem;
import software.aws.toolkits.eclipse.amazonq.views.model.InlineSuggestionCodeReference;

public final class DefaultCodeReferenceLoggingService implements CodeReferenceLoggingService {
    private static DefaultCodeReferenceLoggingService instance;

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm:ss a");

    private DefaultCodeReferenceLoggingService() {
        // Prevent instantiation
    }

    public static synchronized DefaultCodeReferenceLoggingService getInstance() {
        if (instance == null) {
            instance = new DefaultCodeReferenceLoggingService();
        }
        return instance;
    }

    @Override
    public void log(final InlineSuggestionCodeReference codeReference) {
        if (codeReference.references() == null || codeReference.references().length == 0) {
            return;
        }

        for (var reference : codeReference.references()) {
            String message = createInlineSuggestionLogMessage(reference, codeReference.suggestionText(), codeReference.filename(), codeReference.startLine());
            CodeReferenceLogItem logItem = new CodeReferenceLogItem(message);
            CodeReferenceLoggedProvider.notifyCodeReferenceLogged(logItem);
        }
    }

    private String createInlineSuggestionLogMessage(final InlineCompletionReference reference,
                final String suggestionText, final String filename, final int startLine) {
        String formattedDateTime = getNowAsFormattedDate();
        int suggestionTextDepth = suggestionText.split("\n").length;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[%s] Accepted recommendation with code\n", formattedDateTime));
        sb.append(String.format("%s\n", suggestionText));
        sb.append(String.format(
                "provided with reference under %s from repository %s. Added to %s (lines from %d to %d)\n",
                reference.getLicenseName(), reference.getReferenceUrl(), filename, startLine, startLine + suggestionTextDepth));
        String message = sb.toString();

        return message;
    }

    @Override
    public void log(final ChatCodeReference codeReference) {
        if (codeReference.references() == null || codeReference.references().length == 0) {
            return;
        }

        for (ReferenceTrackerInformation reference : codeReference.references()) {
            String message = createChatLogMessage();
            CodeReferenceLogItem logItem = new CodeReferenceLogItem(message);
            CodeReferenceLoggedProvider.notifyCodeReferenceLogged(logItem);
        }
    }

    private String createChatLogMessage() {
        // TODO waiting for message from design
        StringBuilder sb = new StringBuilder();
        String message = "";
        return message;
    }

    private String getNowAsFormattedDate() {
        LocalDateTime now = LocalDateTime.now();
        String formattedDateTime = now.format(formatter);
        return formattedDateTime;
    }
}
