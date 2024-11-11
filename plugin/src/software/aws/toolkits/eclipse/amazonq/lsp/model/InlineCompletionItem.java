// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.model;

public class InlineCompletionItem {

    private String itemId;
    private String insertText;
    private InlineCompletionReference[] references;

    public final String getItemId() {
        return itemId;
    }

    public final void setItemId(final String itemId) {
        this.itemId = itemId;
    }

    public final String getInsertText() {
        return insertText;
    }

    public final void setInsertText(final String insertText) {
        this.insertText = insertText;
    }

    public final InlineCompletionReference[] getReferences() {
        return references;
    }

    public final void setReferences(final InlineCompletionReference[] references) {
        this.references = references;
    }
}
