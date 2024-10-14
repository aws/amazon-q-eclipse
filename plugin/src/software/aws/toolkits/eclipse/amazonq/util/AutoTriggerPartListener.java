// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.texteditor.ITextEditor;

import static software.aws.toolkits.eclipse.amazonq.util.QEclipseEditorUtils.getActiveTextEditor;

public final class AutoTriggerPartListener<T extends IDocumentListener & IAutoTriggerListener>
        implements IPartListener2, IAutoTriggerListener {

    private T docListener;
    private IDocument activeDocument;

    public AutoTriggerPartListener(final T docListener) {
        this.docListener = docListener;
    }

    @Override
    public void partActivated(final IWorkbenchPartReference partRef) {
        System.out.println("Part activated called");
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
        System.out.println("Part deactivated called");
        var part = partRef.getPart(false);
        if (!(part instanceof ITextEditor)) {
            return;
        }
        ITextEditor editor = (ITextEditor) part;
//        var document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        detachDocumentListenerFromLastActiveDocument();
    }

    private void attachDocumentListenerAndUpdateActiveDocument(final ITextEditor editor) {
        var document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        document.addDocumentListener(docListener);
        activeDocument = document;
        System.out.println("odc listener added to doc");
    }

    private void detachDocumentListenerFromLastActiveDocument() {
        if (activeDocument != null) {
            activeDocument.removeDocumentListener(docListener);
            System.out.println("doc listener removed from doc");
        }
    }

    private synchronized void setActiveDocument(final IDocument document) {
        activeDocument = document;
    }

    private synchronized IDocument getActiveDocument() {
        return activeDocument;
    }

    @Override
    public void onStart() {
        // I do not know of a better way to do this currently
        Display.getDefault().asyncExec(() -> {
            while (getActiveTextEditor() == null) {
                System.out.println("Active document is null, sleeping");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            var editor = getActiveTextEditor();
            var document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
            setActiveDocument(document);
            document.addDocumentListener(docListener);
            System.out.println("Document listener added from separate thread");
        });

        docListener.onStart();
    }

    @Override
    public void onShutdown() {
        System.out.println("Part listener on shutdown called");
        docListener.onShutdown();
        if (activeDocument != null) {
            activeDocument.removeDocumentListener(docListener);
        }
    }

}
