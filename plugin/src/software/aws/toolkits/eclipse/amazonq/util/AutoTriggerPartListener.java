// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import static software.aws.toolkits.eclipse.amazonq.util.QEclipseEditorUtils.getActiveTextEditor;
import static software.aws.toolkits.eclipse.amazonq.util.QEclipseEditorUtils.getActiveTextViewer;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.texteditor.ITextEditor;

public final class AutoTriggerPartListener<T extends IDocumentListener & IAutoTriggerListener>
        implements IPartListener2, IAutoTriggerListener {

    private T docListener;
    private IDocument activeDocument;

    public AutoTriggerPartListener(final T docListener) {
        this.docListener = docListener;
    }

    @Override
    public void partActivated(final IWorkbenchPartReference partRef) {
        var part = partRef.getPart(false);
        boolean isEditor = part instanceof ITextEditor;
        if (isEditor) {
            ITextEditor editor = (ITextEditor) part;

            // We should only have at most one listener listening to one document
            // at any given moment. Therefore it would be acceptable to override the
            // listener
            // This is also assuming an active part cannot be activated again.
            attachDocumentListenerAndUpdateActiveDocument(editor);
            return;
        }

        /**
         * When users use ADT plugin and connect to SAP server, the file opened is a customized editor defined by SAP package.
         * com.sap.adt.programs.ui.internal.programs.editors.ProgramEditor
         * , which cause above logic not executed, inline listener is not attached hence inline auto trigger is broken.
         * Here is a monkey patch to fix the issue based on how we currently instrument inline auto trigger.
         * We might need to add different class names if we see more variants of such editor/file showing.
         */
        boolean isProgramEditor = part.getClass().getName().contains("ProgramEditor");
        if (isProgramEditor) {
            ITextEditor e = getActiveTextEditor();
            var viewer = getActiveTextViewer(e);
            if (viewer != null) {
                var document = viewer.getDocument();
                if (document != null) {
                    attachDocumentListenerAndUpdateActiveDocument(document);
                }
            }
        }
    }

    @Override
    public void partDeactivated(final IWorkbenchPartReference partRef) {
        var part = partRef.getPart(false);
        if (!(part instanceof ITextEditor)) {
            return;
        }
        detachDocumentListenerFromLastActiveDocument();
    }

    private void attachDocumentListenerAndUpdateActiveDocument(final ITextEditor editor) {
        var document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        attachDocumentListenerAndUpdateActiveDocument(document);
    }

    private void attachDocumentListenerAndUpdateActiveDocument(final IDocument document) {
        document.addDocumentListener(docListener);
        setActiveDocument(document);
    }

    private void detachDocumentListenerFromLastActiveDocument() {
        if (activeDocument != null) {
            activeDocument.removeDocumentListener(docListener);
        }
    }

    private synchronized void setActiveDocument(final IDocument document) {
        activeDocument = document;
    }

    @Override
    public void onStart() {
        Display.getDefault().timerExec(1000, new Runnable() {
            @Override
            public void run() {
                var editor = getActiveTextEditor();
                if (editor != null) {
                    var document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
                    if (document == null) {
                        Display.getDefault().timerExec(1000, this);
                        return;
                    }
                    setActiveDocument(document);
                    document.addDocumentListener(docListener);
                } else {
                    Display.getDefault().timerExec(1000, this);
                }
            }
        });

        docListener.onStart();
    }

    @Override
    public void onShutdown() {
        docListener.onShutdown();
        if (activeDocument != null) {
            activeDocument.removeDocumentListener(docListener);
        }
    }

}
