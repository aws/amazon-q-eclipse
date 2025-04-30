// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import java.util.List;

import org.eclipse.lsp4j.TextDocumentIdentifier;

import com.fasterxml.jackson.annotation.JsonProperty;

import software.aws.toolkits.eclipse.amazonq.lsp.model.Command;

public final class ChatRequestParams extends BaseChatRequestParams {
    private final String tabId;
    private List<Command> context;

    public ChatRequestParams(
        @JsonProperty("tabId") final String tabId,
        @JsonProperty("prompt") final ChatPrompt prompt,
        @JsonProperty("textDocument") final TextDocumentIdentifier textDocument,
        @JsonProperty("cursorState") final List<CursorState> cursorState,
        @JsonProperty("context") final List<Command> context
    ) {
        super(prompt, textDocument, cursorState);
        this.tabId = tabId;
        this.context = context;
    }

    public String getTabId() {
        return tabId;
    }

    public void setContext(final List<Command> context) {
        this.context = context;
    }

    public List<Command> getContext() {
        return context;
    }
}
