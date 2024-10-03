// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;

import static software.aws.toolkits.eclipse.amazonq.util.QEclipseEditorUtils.shouldIndentVertically;

public class QInlineRendererListener implements PaintListener {

    @Override
    public final void paintControl(final PaintEvent e) {
        var qInvocationSessionInstance = QInvocationSession.getInstance();
        if (!qInvocationSessionInstance.isPreviewingSuggestions()) {
            return;
        }

        var gc = e.gc;
        var widget = qInvocationSessionInstance.getViewer().getTextWidget();
        var invocationLine = widget.getLineAtOffset(qInvocationSessionInstance.getInvocationOffset());
        var segments = qInvocationSessionInstance.getSegments();
        var caretLine = widget.getLineAtOffset(widget.getCaretOffset());
        int numSuggestionLines = qInvocationSessionInstance.getNumSuggestionLines();

        if (shouldIndentVertically(widget, caretLine)
                && qInvocationSessionInstance.isPreviewingSuggestions()) {
            Point textExtent = gc.stringExtent(" ");
            int height = textExtent.y * (numSuggestionLines - (caretLine - invocationLine) - 1);
            qInvocationSessionInstance.setVerticalIndent(caretLine + 1, height);
        } else if (caretLine + 1 == (invocationLine + numSuggestionLines)) {
            qInvocationSessionInstance.unsetVerticalIndent(caretLine + 1);
        }

        for (int i = 0; i < segments.size(); i++) {
            segments.get(i).render(gc, widget.getCaretOffset());
        }
    }
}
