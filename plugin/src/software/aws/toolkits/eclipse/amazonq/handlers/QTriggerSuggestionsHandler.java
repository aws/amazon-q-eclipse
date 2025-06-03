// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.handlers;

import static software.aws.toolkits.eclipse.amazonq.util.QEclipseEditorUtils.getActiveTextEditor;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import software.aws.toolkits.eclipse.amazonq.editor.InMemoryInput;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.QInvocationSession;

public class QTriggerSuggestionsHandler extends AbstractHandler {

    @Override
    public final boolean isEnabled() {
        // TODO: add logic to only trigger on conditions
        return true;
    }

    @Override
    public final synchronized Object execute(final ExecutionEvent event) throws ExecutionException {
        var editor = getActiveTextEditor();
        if (editor == null || editor.getEditorInput() instanceof InMemoryInput) {
            Activator.getLogger().info("Suggestion triggered with no active editor. Returning.");
            return null;
        }

        if (QInvocationSession.getInstance().isActive()) {
            Activator.getLogger().info("Suggestion triggered with existing session active. Returning.");
            return null;
        }

        boolean newSession;
        try {
            newSession = QInvocationSession.getInstance().start(editor);
        } catch (java.util.concurrent.ExecutionException e) {
            Activator.getLogger().error("Session start interrupted", e);
            throw new ExecutionException("Session start interrupted", e);
        }

        if (!newSession) {
            Activator.getLogger().warn("Failed to start suggestion session.");
            return null;
        }

        QInvocationSession.getInstance().invoke();

        return null;
    }
}
