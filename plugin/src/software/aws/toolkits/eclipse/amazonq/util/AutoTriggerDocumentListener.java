// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.NotHandledException;
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
        executionListener = new IExecutionListener() {
            @Override
            public void notHandled(final String commandId, final NotHandledException exception) {
                return;
            }
            @Override
            public void postExecuteFailure(final String commandId, final org.eclipse.core.commands.ExecutionException exception) {
                return;
            }
            @Override
            public void postExecuteSuccess(final String commandId, final Object returnValue) {
                return;
            }
            @Override
            public void preExecute(final String commandId, final ExecutionEvent event) {
                if (commandId.equals(ACCEPTANCE_COMMAND_ID)) {
                    System.out.println("command detected");
                    isChangeInducedByAcceptance = true;
                }
            }
        };
        commandService.addExecutionListener(executionListener);
    }

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
            try {
                qSes.start(editor);
            } catch (ExecutionException e1) {
                return;
            }
        }
        System.out.println("making an auto query");
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
        System.out.println("Doc listener on shutdown called");
        return;
    }
}
