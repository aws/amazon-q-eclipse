// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

import static software.aws.toolkits.eclipse.amazonq.util.QEclipseEditorUtils.getActiveTextEditor;

import java.util.concurrent.ExecutionException;

public final class AutoTriggerDocumentListener implements IDocumentListener, IAutoTriggerListener {
    private static final String UNDO_COMMAND_ID = "org.eclipse.ui.edit.undo";

    private ThreadLocal<Boolean> isChangeInducedByUndo = ThreadLocal.withInitial(() -> false);
    private IExecutionListener commandListener;

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
        if (!Activator.getLoginService().getAuthState().isLoggedIn()) {
            return false;
        }

        if (e.getText().length() <= 0) {
            return false;
        }

        if (session.isPreviewingSuggestions() || session.isDecisionMade()) {
            return false;
        }

        if (isChangeInducedByUndo.get()) {
            isChangeInducedByUndo.set(false);
            return false;
        }

        // TODO: implement other logic to prevent unnecessary firing
        return true;
    }

    @Override
    public void onStart() {
        ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
        commandListener = QEclipseEditorUtils.getAutoTriggerExecutionListener((commandId) -> undoCommandListenerCallback(commandId));
        commandService.addExecutionListener(commandListener);
        return;
    }

    @Override
    public void onShutdown() {
        if (commandListener != null) {
            ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
            commandService.removeExecutionListener(commandListener);
        }
        return;
    }

    private void undoCommandListenerCallback(final String commandId) {
        if (commandId.equals(UNDO_COMMAND_ID)) {
            isChangeInducedByUndo.set(true);
        }
    }
}
