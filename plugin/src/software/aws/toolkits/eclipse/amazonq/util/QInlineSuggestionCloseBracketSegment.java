// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.util;

import static software.aws.toolkits.eclipse.amazonq.util.QConstants.Q_INLINE_HINT_TEXT_COLOR;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Display;

public class QInlineSuggestionCloseBracketSegment implements IQInlineSuggestionSegment {
    private QInlineSuggestionOpenBracketSegment openBracket;
    public char symbol;
    private int caretOffset;
    private int lineInSuggestion;
    private String text;
    private Color color = new Color(Display.getCurrent(), 255, 0, 0);

    public QInlineSuggestionCloseBracketSegment(int caretOffset, int lineInSuggestion, String text, char symbol) {
        this.caretOffset = caretOffset;
        this.symbol = symbol;
        this.lineInSuggestion = lineInSuggestion;
        this.text = text;
    }

    public void pairUp(QInlineSuggestionOpenBracketSegment openBracket) {
        this.openBracket = openBracket;
        if (!openBracket.hasPairedUp()) {
            this.openBracket.pairUp(this);
        }
    }

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
        int invocationLine = widget.getLineAtOffset(qInvocationSessionInstance.getInvocationOffset());
        int lineHt = widget.getLineHeight();
        int fontHt = gc.getFontMetrics().getHeight();
        y = (invocationLine + lineInSuggestion + 1) * lineHt - fontHt;
        x = gc.textExtent(text).x;

        gc.setForeground(color);
        gc.setFont(qInvocationSessionInstance.getInlineTextFont());
        gc.drawText(String.valueOf(symbol), x, y, true);
    }
}
