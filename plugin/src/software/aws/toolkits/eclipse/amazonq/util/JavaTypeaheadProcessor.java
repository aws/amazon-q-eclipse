// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.texteditor.ITextEditor;

public class JavaTypeaheadProcessor implements IQInlineTypeaheadProcessor {
    private StyledText widget;
    private ITextEditor editor;
    private IDocument doc;

    public JavaTypeaheadProcessor(final ITextEditor editor) {
        this.editor = editor;
        ITextViewer textViewer = (ITextViewer) editor.getAdapter(ITextViewer.class);
        widget = textViewer.getTextWidget();
        doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
    }

    @Override
    public int getNewDistanceTraversedOnDelete(int inputLength, int currentDistanceTraversed,
            IQInlineBracket[] brackets) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean preprocessDocumentChangedBufferAndMaybeModifyDocument(int distanceTraversed, int eventOffset,
            String input) throws BadLocationException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean preprocessBufferVerifyKeyzbufferAndMaybeModifyDocument(int distanceTraversed, char input,
            IQInlineBracket[] brackets) throws BadLocationException {
        // TODO Auto-generated method stub
        return false;
    }

}
