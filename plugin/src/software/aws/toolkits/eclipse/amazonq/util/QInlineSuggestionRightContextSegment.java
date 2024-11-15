package software.aws.toolkits.eclipse.amazonq.util;

import static software.aws.toolkits.eclipse.amazonq.util.QConstants.Q_INLINE_HINT_TEXT_COLOR;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

public final class QInlineSuggestionRightContextSegment implements IQInlineSuggestionSegment {
    private String text;
    private String firstLineInSuggestion;

    public QInlineSuggestionRightContextSegment(final String text, final String firstLineInSuggestion) {
        this.text = text;
        this.firstLineInSuggestion = firstLineInSuggestion;
    }

    @Override
    public void render(final GC gc, final int currentCaretOffset) {
        QInvocationSession session = QInvocationSession.getInstance();
        if (session == null) {
            return;
        }
        StyledText widget = session.getViewer().getTextWidget();

        int x;
        int y;
        int invocationLine = widget.getLineAtOffset(session.getInvocationOffset());
        int lineHt = widget.getLineHeight();
        int fontHt = gc.getFontMetrics().getHeight();
        y = (invocationLine + 1) * lineHt - fontHt;
        int scrollOffsetY = widget.getTopPixel();
        y -= scrollOffsetY;
        x = widget.getLocationAtOffset(session.getInvocationOffset()).x + gc.textExtent(firstLineInSuggestion).x;
        gc.setForeground(Q_INLINE_HINT_TEXT_COLOR);
        gc.setFont(widget.getFont());
        gc.drawText(text, x, y, false);
        FontMetrics fontMetrics = gc.getFontMetrics();
        int lineY = y + fontMetrics.getAscent() / 2;
        lineY -= scrollOffsetY;
        Point textExtent = gc.textExtent(text);
        int textWidth = textExtent.x;
        gc.drawLine(x, lineY, x + textWidth, lineY);
    }

    @Override
    public void cleanUp() {
    }
}
