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
        System.out.println("Document change: " + e.getText());
        if (!shouldSendQuery(e)) {
            return;
        }

        var qSes = QInvocationSession.getInstance();
        if (!qSes.isActive()) {
            var editor = getActiveTextEditor();
            qSes.start(editor);
        }
        qSes.invoke();
    }

    private boolean shouldSendQuery(final DocumentEvent e) {
        if (e.getText().length() <= 0) {
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
        return;
    }
}
