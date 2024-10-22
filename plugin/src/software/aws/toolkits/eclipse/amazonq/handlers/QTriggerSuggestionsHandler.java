// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.QInvocationSession;

import static software.aws.toolkits.eclipse.amazonq.util.QEclipseEditorUtils.getActiveTextEditor;

public class QTriggerSuggestionsHandler extends AbstractHandler {

    @Override
    public final boolean isEnabled() {
        // TODO: add logic to only trigger on conditions
        return !QInvocationSession.getInstance().isActive();
    }

    @Override
    public final Object execute(final ExecutionEvent event) throws ExecutionException {
        var editor = getActiveTextEditor();
        if (editor == null) {
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
            System.out.println("session already started, not starting another one");
            return null;
        }

        QInvocationSession.getInstance().invoke();

        System.out.println("TriggerSuggestionsHandler called");
        return null;
    }
}
