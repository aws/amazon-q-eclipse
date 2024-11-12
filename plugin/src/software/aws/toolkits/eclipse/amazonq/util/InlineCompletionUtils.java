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
            final int invocationOffset, final InlineCompletionTriggerKind triggerKind) throws BadLocationException {
        System.out.println("Param made with invocation offset of " + invocationOffset);
        var document = viewer.getDocument();

        var openFileUri = QEclipseEditorUtils.getOpenFileUri(editor.getEditorInput());

        var params = new InlineCompletionParams();
        openFileUri.ifPresent(filePathUri -> {
            params.setTextDocument(new TextDocumentIdentifier(filePathUri));
        });

        var inlineCompletionContext = new InlineCompletionContext();
        inlineCompletionContext.setTriggerKind(triggerKind);

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
