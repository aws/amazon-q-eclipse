// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.ui.texteditor.ITextEditor;

import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionContext;
import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionParams;
import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionTriggerKind;

public final class InlineCompletionUtils {

    private InlineCompletionUtils() {
        // Prevent instantiation
    }

    public static InlineCompletionParams cwParamsFromContext(final ITextEditor editor, final ITextViewer viewer,
            final int invocationOffset) throws BadLocationException {
        var document = viewer.getDocument();

        var openFilePath = QEclipseEditorUtils.getOpenFilePath(editor.getEditorInput());

        var params = new InlineCompletionParams();
        var identifier = new TextDocumentIdentifier();
        identifier.setUri("file://" + openFilePath);
        params.setTextDocument(identifier);

        var inlineCompletionContext = new InlineCompletionContext();
        inlineCompletionContext.setTriggerKind(InlineCompletionTriggerKind.Invoke);

        params.setContext(inlineCompletionContext);

        var invocationPosition = new Position();
        var startLine = document.getLineOfOffset(invocationOffset);
        var lineOffset = invocationOffset - document.getLineOffset(startLine);
        invocationPosition.setLine(startLine);
        invocationPosition.setCharacter(lineOffset);
        params.setPosition(invocationPosition);
        return params;
    }
}
