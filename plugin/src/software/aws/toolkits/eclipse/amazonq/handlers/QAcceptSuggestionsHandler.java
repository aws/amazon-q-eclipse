// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import software.aws.toolkits.eclipse.amazonq.util.PluginLogger;
import software.aws.toolkits.eclipse.amazonq.util.QInvocationSession;

public class QAcceptSuggestionsHandler extends AbstractHandler {

    @Override
    public final boolean isEnabled() {
        return QInvocationSession.getInstance().isPreviewingSuggestions();
    }

    @Override
    public final Object execute(final ExecutionEvent event) throws ExecutionException {
        var suggestion = QInvocationSession.getInstance().getCurrentSuggestion();
        var widget = QInvocationSession.getInstance().getViewer().getTextWidget();
        Display display = widget.getDisplay();
        display.syncExec(() -> this.insertSuggestion(suggestion));
        return null;
    }

    private void insertSuggestion(final String suggestion) {
        try {
            var viewer = QInvocationSession.getInstance().getViewer();
            IDocument doc = viewer.getDocument();
            var widget = viewer.getTextWidget();
            var insertOffset = widget.getCaretOffset();
            doc.replace(insertOffset, 0, suggestion);
            widget.setCaretOffset(insertOffset + suggestion.length());
            QInvocationSession.getInstance().transitionToDecisionMade();
            QInvocationSession.getInstance().end();
        } catch (BadLocationException e) {
            PluginLogger.error(e.toString());
        }
    }
}
