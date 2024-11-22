// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.util;

import static software.aws.toolkits.eclipse.amazonq.util.QConstants.Q_INLINE_HINT_TEXT_COLOR;

import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.GlyphMetrics;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

public final class QInlineSuggestionNormalSegment implements IQInlineSuggestionSegment {
    private int startCaretOffset;
    private int endCaretOffset;
    private int lineInSuggestion;
    private String text;
    private StyleRange styleRange = new StyleRange();

    public QInlineSuggestionNormalSegment(final int startCaretPosition, final int endCaretPosition,
            final int lineInSuggestion, final String text) {
        this.text = text;
        this.startCaretOffset = startCaretPosition;
        this.endCaretOffset = endCaretPosition;
        this.lineInSuggestion = lineInSuggestion;
    }

    @Override
    public void render(final GC gc, final int currentCaretOffset) {
        if (currentCaretOffset > endCaretOffset) {
            return;
        }
        var qInvocationSessionInstance = QInvocationSession.getInstance();
        if (qInvocationSessionInstance == null) {
            return;
        }
        var widget = qInvocationSessionInstance.getViewer().getTextWidget();

        int x;
        int y;
        String textToRender;
        int invocationLine = widget.getLineAtOffset(qInvocationSessionInstance.getInvocationOffset());
        int lineHt = widget.getLineHeight();
        int fontHt = gc.getFontMetrics().getHeight();
        y = (invocationLine + lineInSuggestion + 1) * lineHt - fontHt;

        int idxInLine = currentCaretOffset - startCaretOffset;
        if (lineInSuggestion == 0) {
            x = widget.getLocationAtOffset(widget.getCaretOffset()).x;
            textToRender = text.substring(idxInLine);
            int curLineInDoc = widget.getLineAtOffset(currentCaretOffset);
            int lineIdx = currentCaretOffset - widget.getOffsetAtLine(invocationLine);
            String contentInLine = widget.getLine(curLineInDoc);
            String rightCtxInLine = contentInLine.substring(lineIdx);
            if (!rightCtxInLine.isBlank() && !text.endsWith("\n")) {
                styleRange.start = currentCaretOffset;
                styleRange.length = 1;
                styleRange.metrics = new GlyphMetrics(0, 0, gc.textExtent(textToRender).x + gc.textExtent(" ").x);
                styleRange.foreground = widget.getBackground();
                widget.setStyleRange(styleRange);
                // also include the character right of the caret that is covered by the glyph
                textToRender += contentInLine.charAt(lineIdx);
            }
        } else if (currentCaretOffset <= startCaretOffset) {
            textToRender = text;
            x = widget.getLeftMargin();
        } else {
            x = gc.textExtent(text.substring(0, idxInLine)).x;
            textToRender = text.substring(idxInLine);
        }

        int scrollOffsetY = widget.getTopPixel();
        y -= scrollOffsetY;

        gc.setForeground(Q_INLINE_HINT_TEXT_COLOR);
        gc.setFont(qInvocationSessionInstance.getInlineTextFont());
        gc.drawText(textToRender, x, y, true);
    }

    @Override
    public void cleanUp() {
        QInvocationSession session = QInvocationSession.getInstance();
        if (!session.isActive()) {
            return;
        }
        StyledText widget = session.getViewer().getTextWidget();
        styleRange.metrics = new GlyphMetrics(0, 0, 0);
        try {
            widget.setStyleRange(styleRange);
        } catch (Exception e) {
            Activator.getLogger().warn("Error cleaning up glyph: " + e);
        }
    }
}
