// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import java.util.List;

import org.eclipse.lsp4j.TextDocumentIdentifier;

import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class BaseChatRequestParams {
    private final ChatPrompt prompt;
    private String partialResultToken;
    private TextDocumentIdentifier textDocument;
    private List<CursorState> cursorState;

    protected BaseChatRequestParams(
            @JsonProperty("prompt") final ChatPrompt prompt,
            @JsonProperty("textDocument") final TextDocumentIdentifier textDocument,
            @JsonProperty("cursorState") final List<CursorState> cursorState
        ) {
            this.prompt = prompt;
            this.textDocument = textDocument;
            this.cursorState = (cursorState != null) ? cursorState : List.of();
    }

    public final ChatPrompt getPrompt() {
        return prompt;
    }

    public final String getPartialResultToken() {
        return partialResultToken;
    }

    public final void setPartialResultToken(final String partialResultToken) {
        this.partialResultToken = partialResultToken;
    }

    public final List<CursorState> getCursorState() {
        return cursorState;
    }

    public final void setCursorState(final List<CursorState> cursorState) {
        this.cursorState = cursorState;
    }

    public final TextDocumentIdentifier getTextDocument() {
        return textDocument;
    }

    public final void setTextDocument(final TextDocumentIdentifier textDocument) {
        this.textDocument = textDocument;
    }
}
