package software.aws.toolkits.eclipse.amazonq.util;

import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionItem;

@FunctionalInterface
public interface CodeReferenceAcceptanceCallback {
    void onCallback(InlineCompletionItem suggestionItem, int startLine);
}
