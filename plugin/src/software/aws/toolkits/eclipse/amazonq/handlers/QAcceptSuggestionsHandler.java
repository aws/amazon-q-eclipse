// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.QEclipseEditorUtils;
import software.aws.toolkits.eclipse.amazonq.util.QInvocationSession;

public class QAcceptSuggestionsHandler extends AbstractHandler {

    @Override
    public final boolean isEnabled() {
        return QInvocationSession.getInstance().isPreviewingSuggestions();
    }

    @Override
    public final synchronized Object execute(final ExecutionEvent event) throws ExecutionException {
        var suggestion = QInvocationSession.getInstance().getCurrentSuggestion();
        var widget = QInvocationSession.getInstance().getViewer().getTextWidget();
        var session = QInvocationSession.getInstance();
        // mark current suggestion as accepted
        session.setAccepted(suggestion.getItemId());
        session.setSuggestionAccepted(true);
        session.transitionToDecisionMade();
        Display display = widget.getDisplay();
        display.syncExec(() -> this.insertSuggestion(suggestion.getInsertText()));
        return null;
    }

    private void insertSuggestion(final String suggestion) {
        try {
            var qSes = QInvocationSession.getInstance();
            var viewer = qSes.getViewer();
            IDocument doc = viewer.getDocument();
            var widget = viewer.getTextWidget();
            var insertOffset = widget.getCaretOffset();
            int adjustedOffset = QEclipseEditorUtils.getOffsetInFullyExpandedDocument(viewer, insertOffset);
            int startIdx = widget.getCaretOffset() - qSes.getInvocationOffset();
            String adjustedSuggestion = suggestion.substring(startIdx);
            int outstandingPadding = qSes.getOutstandingPadding();
            doc.replace(adjustedOffset, outstandingPadding, adjustedSuggestion);
            widget.setCaretOffset(insertOffset + adjustedSuggestion.length());
            QInvocationSession.getInstance().getViewer().getTextWidget().redraw();
            QInvocationSession.getInstance().executeCallbackForCodeReference();
            QInvocationSession.getInstance().end();
        } catch (BadLocationException e) {
            Activator.getLogger().error(e.toString());
        }
    }
}
