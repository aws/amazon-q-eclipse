// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionItem;

public final class QSuggestionContext {
    private InlineCompletionItem inlineCompletionItem;
    private QSuggestionState state;

    public QSuggestionContext(final InlineCompletionItem inlineCompletionItem) {
        this.inlineCompletionItem = inlineCompletionItem;
        state = QSuggestionState.UNSEEN;
    }

    public InlineCompletionItem getInlineCompletionItem() {
        return inlineCompletionItem;
    }

    public QSuggestionState getState() {
        return state;
    }

}
