// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.texteditor.ITextEditor;

public final class AutoTriggerPartListener implements IPartListener2 {

    private IDocumentListener docListener;
    private IDocument activeDocument;

    public AutoTriggerPartListener(final IDocumentListener docListener) {
        this.docListener = docListener;
    }

    @Override
    public void partActivated(final IWorkbenchPartReference partRef) {
        var part = partRef.getPart(false);
        if (!(part instanceof ITextEditor)) {
            return;
        }
        ITextEditor editor = (ITextEditor) part;

        // We should only have at most one listener listening to one document
        // at any given moment. Therefore it would be acceptable to override the
        // listener
        // This is also assuming an active part cannot be activated again.
        attachDocumentListenerAndUpdateActiveDocument(editor);
    }

    @Override
    public void partDeactivated(final IWorkbenchPartReference partRef) {
        var part = partRef.getPart(false);
        if (!(part instanceof ITextEditor)) {
            return;
        }
        ITextEditor editor = (ITextEditor) part;
        var document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        if (activeDocument == document) {
            detachDocumentListenerFromLastActiveDocument();
        }
    }

    private void attachDocumentListenerAndUpdateActiveDocument(final ITextEditor editor) {
        var document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        document.addDocumentListener(docListener);
        activeDocument = document;
    }

    private void detachDocumentListenerFromLastActiveDocument() {
        if (activeDocument != null) {
            activeDocument.removeDocumentListener(docListener);
        }
    }

}
