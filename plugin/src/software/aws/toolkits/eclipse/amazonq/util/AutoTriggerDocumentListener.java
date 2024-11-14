// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

import static software.aws.toolkits.eclipse.amazonq.util.QEclipseEditorUtils.getActiveTextEditor;

import java.util.concurrent.ExecutionException;

public final class AutoTriggerDocumentListener implements IDocumentListener, IAutoTriggerListener {
    private static final String ACCEPTANCE_COMMAND_ID = "software.aws.toolkits.eclipse.amazonq.commands.acceptSuggestions";

    private boolean isChangeInducedByAcceptance = false;
    private IExecutionListener executionListener = null;

    public AutoTriggerDocumentListener() {
        ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
        executionListener = QEclipseEditorUtils.getAutoTriggerExecutionListener((commandId) -> {
            if (commandId.equals(ACCEPTANCE_COMMAND_ID)) {
                isChangeInducedByAcceptance = true;
            }
        });
        commandService.addExecutionListener(executionListener);
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

        if (session.isPreviewingSuggestions()) {
            return false;
        }

        if (isChangeInducedByAcceptance) {
            // It is acceptable to alter the state here because:
            // - This listener is the only thing that is consuming this state
            // - `documentChanged` is called on a single thread. And therefore `shouldSendQuery` is also called on a single thread.
            isChangeInducedByAcceptance = false;
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
