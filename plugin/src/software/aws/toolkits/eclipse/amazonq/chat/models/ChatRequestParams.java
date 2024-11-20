// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.chat.models;

import java.util.List;

import org.eclipse.lsp4j.TextDocumentIdentifier;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class ChatRequestParams {
    private final String tabId;
    private final ChatPrompt prompt;
    private String partialResultToken;
    private TextDocumentIdentifier textDocument;
    private List<CursorState> cursorState;

    public ChatRequestParams(
        @JsonProperty("tabId") final String tabId,
        @JsonProperty("prompt") final ChatPrompt prompt,
        @JsonProperty("textDocument") final TextDocumentIdentifier textDocument,
        @JsonProperty("cursorState") final CursorState cursorState
    ) {
        this.tabId = tabId;
        this.prompt = prompt;
        this.textDocument = textDocument;
        this.cursorState = List.of(cursorState);
    }

    public String getTabId() {
        return tabId;
    }

    public ChatPrompt getPrompt() {
        return prompt;
    }

    public String getPartialResultToken() {
        return partialResultToken;
    }

    public void setPartialResultToken(final String partialResultToken) {
        this.partialResultToken = partialResultToken;
    }

    public List<CursorState> getCursorState() {
        return cursorState;
    }

    public void setCursorState(final List<CursorState> cursorState) {
        this.cursorState = cursorState;
    }

    public TextDocumentIdentifier getTextDocument() {
        return textDocument;
    }

    public void setTextDocument(final TextDocumentIdentifier textDocument) {
        this.textDocument = textDocument;
    }
}

