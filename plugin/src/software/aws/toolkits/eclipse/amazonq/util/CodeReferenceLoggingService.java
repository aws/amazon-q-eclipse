package software.aws.toolkits.eclipse.amazonq.util;

import software.aws.toolkits.eclipse.amazonq.views.model.ChatCodeReference;
import software.aws.toolkits.eclipse.amazonq.views.model.InlineSuggestionCodeReference;

public interface CodeReferenceLoggingService {
    void log(InlineSuggestionCodeReference codeReference);
    void log(ChatCodeReference codeReference);
}
