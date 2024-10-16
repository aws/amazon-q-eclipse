// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;

import static software.aws.toolkits.eclipse.amazonq.util.QEclipseEditorUtils.getActiveTextEditor;

public final class AutoTriggerDocumentListener implements IDocumentListener, IAutoTriggerListener {

    @Override
    public void documentAboutToBeChanged(final DocumentEvent e) {
        return;
    }

    @Override
    public void documentChanged(final DocumentEvent e) {
        var qSes = QInvocationSession.getInstance();
        if (!shouldSendQuery(e, qSes)) {
            return;
        }
        if (!qSes.isActive()) {
            var editor = getActiveTextEditor();
            qSes.start(editor);
        }
        qSes.invoke(qSes.getViewer().getTextWidget().getCaretOffset() + e.getText().length());
    }

    private boolean shouldSendQuery(final DocumentEvent e, final QInvocationSession session) {
        if (e.getText().length() <= 0) {
            return false;
        }

        if (session.isPreviewingSuggestions() || session.isDecisionMade()) {
            return false;
        }

        // TODO: implement other logic to prevent unnecessary firing
        return true;
    }

    @Override
    public void onStart() {
        return;
    }

    @Override
    public void onShutdown() {
        System.out.println("Doc listener on shutdown called");
        return;
    }
}
