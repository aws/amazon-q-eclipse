// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.util;

import static software.aws.toolkits.eclipse.amazonq.util.QConstants.Q_INLINE_HINT_TEXT_COLOR;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

public class QInlineSuggestionOpenBracketSegment implements IQInlineSuggestionSegment {
    private QInlineSuggestionCloseBracketSegment closeBracket;
    public char symbol;
    private int caretOffset;
    private int idxInLine;
    private int lineInSuggestion;

    public QInlineSuggestionOpenBracketSegment(int caretOffset, int lineInSuggestion, int idxInLine, char symbol) {
        this.caretOffset = caretOffset;
        this.symbol = symbol;
        this.lineInSuggestion = lineInSuggestion;
        this.idxInLine = idxInLine;
    }

    public void pairUp(QInlineSuggestionCloseBracketSegment closeBracket) {
        this.closeBracket = closeBracket;
        if (!closeBracket.hasPairedUp()) {
            closeBracket.pairUp(this);
        }
    }

    public boolean isAMatch(QInlineSuggestionCloseBracketSegment closeBracket) {
        switch (symbol) {
        case '<':
            return closeBracket.symbol == '>';
        case '{':
            return closeBracket.symbol == '}';
        case '(':
            return closeBracket.symbol == ')';
        case '"':
            return closeBracket.symbol == '"';
        case '\'':
            return closeBracket.symbol == '\'';
        case '[':
            return closeBracket.symbol == ']';
        default:
            return false;
        }
    }

    public boolean hasPairedUp() {
        return closeBracket != null;
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
        int invocationLine = widget.getLineAtOffset(qInvocationSessionInstance.getInvocationOffset());
        int lineHt = widget.getLineHeight();
        int fontHt = gc.getFontMetrics().getHeight();
        int fontWd = (int) gc.getFontMetrics().getAverageCharacterWidth();
        y = (invocationLine + lineInSuggestion + 1) * lineHt - fontHt;
        x = widget.getLeftMargin() + fontWd * idxInLine;

        gc.setForeground(Q_INLINE_HINT_TEXT_COLOR);
        gc.setFont(qInvocationSessionInstance.getInlineTextFont());
        gc.drawText(String.valueOf(symbol), x, y, true);
    }
}
