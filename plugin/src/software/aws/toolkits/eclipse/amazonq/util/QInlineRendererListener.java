// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;

import static software.aws.toolkits.eclipse.amazonq.util.QConstants.Q_INLINE_HINT_TEXT_COLOR;
import static software.aws.toolkits.eclipse.amazonq.util.QEclipseEditorUtils.shouldIndentVertically;

import java.util.Arrays;

public class QInlineRendererListener implements PaintListener {
    private int currentLine = -1;
    private int offsetAtCurrentLine = -1;

    @Override
    public final void paintControl(final PaintEvent e) {
        var qInvocationSessionInstance = QInvocationSession.getInstance();
        if (!qInvocationSessionInstance.isPreviewingSuggestions()) {
            return;
        }

        var gc = e.gc;
        gc.setForeground(Q_INLINE_HINT_TEXT_COLOR);
        gc.setFont(QInvocationSession.getInstance().getInlineTextFont());
        var widget = QInvocationSession.getInstance().getViewer().getTextWidget();

        var location = widget.getLocationAtOffset(widget.getCaretOffset());
        var suggestion = QInvocationSession.getInstance().getCurrentSuggestion().getInsertText();
        int invocationOffset = QInvocationSession.getInstance().getInvocationOffset();
        var suggestionParts = suggestion.split("\\R");

        int currentOffset = widget.getCaretOffset();
        int lineOffset = widget.getLineAtOffset(currentOffset);
        int originalLine = widget.getLineAtOffset(invocationOffset);
        int currentLineInSuggestion = lineOffset - originalLine;
        if (currentLine < lineOffset) {
            // this accounts for a traversal "downwards" as user types
            currentLine = lineOffset;
            offsetAtCurrentLine = currentOffset;
            qInvocationSessionInstance.setHeadOffsetAtLine(lineOffset, currentOffset);
        } else if (currentLine > lineOffset) {
            // this accounts for a traversal "upwards" as user backspaces
            currentLine = lineOffset;
            offsetAtCurrentLine = qInvocationSessionInstance.getHeadOffsetAtLine(lineOffset);
        }

        int renderHeadIndex = currentOffset - offsetAtCurrentLine;
        String[] remainderArray = Arrays.copyOfRange(suggestionParts, currentLineInSuggestion + 1,
                suggestionParts.length);
        String remainder = String.join("\n", remainderArray);

        // Draw first line inline
        String firstLine = renderHeadIndex >= 0 ? suggestionParts[currentLineInSuggestion].trim()
                : suggestionParts[currentLineInSuggestion];
        int xLoc = renderHeadIndex >= 0 ? location.x : widget.getLeftMargin();
        if (renderHeadIndex < firstLine.length()) {
            gc.drawText(renderHeadIndex >= 0 ? firstLine.substring(renderHeadIndex) : firstLine, xLoc, location.y,
                    true);
        }

        // Draw other lines inline
        if (!remainder.isEmpty()) {
            // For last line case doesn't need to indent next line vertically
            var caretLine = widget.getLineAtOffset(widget.getCaretOffset());
            if (shouldIndentVertically(widget, caretLine) && qInvocationSessionInstance.isPreviewingSuggestions()) {
                // when showing the suggestion need to add next line indent
                Point textExtent = gc.stringExtent(" ");
                int height = textExtent.y * remainder.split("\\R").length;
                qInvocationSessionInstance.setVerticalIndent(caretLine + 1, height);
            }

            int lineHt = widget.getLineHeight();
            int fontHt = gc.getFontMetrics().getHeight();
            int x = widget.getLeftMargin();
            int y = location.y + lineHt * 2 - fontHt;
            gc.drawText(remainder, x, y, true);
        } else {
            int line = widget.getLineAtOffset(widget.getCaretOffset());
            qInvocationSessionInstance.unsetVerticalIndent(line + 1);
        }
    }

}
