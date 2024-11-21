// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;

import static software.aws.toolkits.eclipse.amazonq.util.QEclipseEditorUtils.getActiveTextEditor;

import java.util.concurrent.ExecutionException;

public final class AutoTriggerDocumentListener implements IDocumentListener, IAutoTriggerListener {
    public AutoTriggerDocumentListener() {
    }

    @Override
    public void documentAboutToBeChanged(final DocumentEvent e) {
        return;
    }

    @Override
    public synchronized void documentChanged(final DocumentEvent e) {
        var qSes = QInvocationSession.getInstance();
        if (!shouldSendQuery(e, qSes)) {
            return;
        }
        if (!qSes.isActive()) {
            var editor = getActiveTextEditor();
            try {
                qSes.start(editor);
            } catch (ExecutionException e1) {
                return;
            }
        }
        qSes.invoke(qSes.getViewer().getTextWidget().getCaretOffset(), e.getText().length());
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
        return;
    }
}
