// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.util;

import static software.aws.toolkits.eclipse.amazonq.util.QConstants.Q_INLINE_HINT_TEXT_COLOR;

import org.eclipse.swt.graphics.GC;

public final class QInlineSuggestionNormalSegment implements IQInlineSuggestionSegment {
    private int startCaretOffset;
    private int endCaretOffset;
    private int lineInSuggestion;
    private String text;

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
        } else if (currentCaretOffset <= startCaretOffset) {
            textToRender = text;
            x = widget.getLeftMargin();
        } else {
            x = gc.textExtent(text.substring(0, idxInLine)).x + gc.textExtent(" ").x / 4;
            textToRender = text.substring(idxInLine);
        }

        gc.setForeground(Q_INLINE_HINT_TEXT_COLOR);
        gc.setFont(qInvocationSessionInstance.getInlineTextFont());
        gc.drawText(textToRender, x, y, true);
    }
}
