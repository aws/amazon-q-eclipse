// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.util;

import static software.aws.toolkits.eclipse.amazonq.util.QConstants.Q_INLINE_HINT_TEXT_COLOR;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Display;

public class QInlineSuggestionCloseBracketSegment implements IQInlineSuggestionSegment, IQInlineBracket {
    private QInlineSuggestionOpenBracketSegment openBracket;
    public char symbol;
    public int caretOffset;
    private int lineInSuggestion;
    private String text;
    private Color color = new Color(Display.getCurrent(), 255, 0, 0);

    public QInlineSuggestionCloseBracketSegment(int caretOffset, int lineInSuggestion, String text, char symbol) {
        this.caretOffset = caretOffset;
        this.symbol = symbol;
        this.lineInSuggestion = lineInSuggestion;
        this.text = text;
    }

    @Override
    public void pairUp(IQInlineBracket openBracket) {
        this.openBracket = (QInlineSuggestionOpenBracketSegment) openBracket;
        if (!openBracket.hasPairedUp()) {
            this.openBracket.pairUp(this);
        }
    }

    @Override
    public boolean hasPairedUp() {
        return openBracket != null;
    }

    @Override
    public void render(GC gc, int currentCaretOffset) {
        if (currentCaretOffset > caretOffset) {
            return;
        }
        var qInvocationSessionInstance = QInvocationSession.getInstance();
        if (qInvocationSessionInstance == null) {
            return;
        }
        var widget = qInvocationSessionInstance.getViewer().getTextWidget();

        int x, y;
        int invocationOffset = qInvocationSessionInstance.getInvocationOffset();
        int invocationLine = widget.getLineAtOffset(invocationOffset);
        int lineHt = widget.getLineHeight();
        int fontHt = gc.getFontMetrics().getHeight();
        // educated guess:
        int endPadding = gc.getAdvanceWidth(symbol) / 4;
        y = (invocationLine + lineInSuggestion + 1) * lineHt - fontHt;
        x = gc.textExtent(text).x + endPadding;
        if (lineInSuggestion == 0) {
            x += widget.getLocationAtOffset(invocationOffset).x;
        }

        if (currentCaretOffset > openBracket.caretOffset) {
            Font typedFont = widget.getFont();
//            Color typedColor = widget.getForeground();
            gc.setForeground(color);
            gc.setFont(typedFont);
        } else {
            gc.setForeground(Q_INLINE_HINT_TEXT_COLOR);
            gc.setFont(qInvocationSessionInstance.getInlineTextFont());
        }
        gc.drawText(String.valueOf(symbol), x, y, true);
    }

    @Override
    public void onTypeOver() {
        openBracket.setResolve(true);
    }

    @Override
    public void onDelete() {
        openBracket.setResolve(true);
    }

    @Override
    public String getAutoCloseContent(boolean isBracketSetToAutoClose, boolean isBracesSetToAutoClose,
            boolean isStringSetToAutoClose) {
        // This is a noop for close brackets
        return null;
    }

    @Override
    public int getRelevantOffset() {
        return caretOffset;
    }

    @Override
    public char getSymbol() {
        return symbol;
    }
}
