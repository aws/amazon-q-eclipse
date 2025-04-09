// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.chat.models;

import java.util.List;

import org.eclipse.lsp4j.TextDocumentIdentifier;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class InlineChatRequestParams extends BaseChatRequestParams {

    public InlineChatRequestParams(
        @JsonProperty("prompt") final ChatPrompt prompt,
        @JsonProperty("textDocument") final TextDocumentIdentifier textDocument,
        @JsonProperty("cursorState") final List<CursorState> cursorState
    ) {
        super(prompt, textDocument, cursorState);
    }
}

