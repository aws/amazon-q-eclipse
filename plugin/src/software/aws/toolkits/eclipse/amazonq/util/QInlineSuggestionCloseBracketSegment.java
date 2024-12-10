// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.util;

import static software.aws.toolkits.eclipse.amazonq.util.QConstants.Q_INLINE_HINT_TEXT_COLOR;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.widgets.Display;

public final class QInlineSuggestionCloseBracketSegment implements IQInlineSuggestionSegment, IQInlineBracket {
    private QInlineSuggestionOpenBracketSegment openBracket;
    private char symbol;
    private int caretOffset;
    private int lineInSuggestion;
    private String text;
    private Font adjustedTypedFont;
    private TextLayout layout;
    private TextLayout measureLayout;
    private boolean isMacOS;

    public QInlineSuggestionCloseBracketSegment(final int caretOffset, final int lineInSuggestion, final String text,
            final char symbol, final boolean isMacOS) {
        this.caretOffset = caretOffset;
        this.symbol = symbol;
        this.lineInSuggestion = lineInSuggestion;
        this.text = text;
        this.layout = isMacOS ? null : new TextLayout(Display.getCurrent());
        this.isMacOS = isMacOS;

        var qInvocationSessionInstance = QInvocationSession.getInstance();
        adjustedTypedFont = qInvocationSessionInstance.getBoldInlineFont();
        if (!isMacOS) {
            int[] tabStops = qInvocationSessionInstance.getViewer().getTextWidget().getTabStops();
            measureLayout = new TextLayout(Display.getCurrent());
            measureLayout.setText(text);
            measureLayout.setFont(qInvocationSessionInstance.getInlineTextFont());
            measureLayout.setTabs(tabStops);
        }
    }

    @Override
    public void pairUp(final IQInlineBracket openBracket) {
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
    public void render(final GC gc, final int currentCaretOffset) {
        if (currentCaretOffset > caretOffset) {
            return;
        }
        var qInvocationSessionInstance = QInvocationSession.getInstance();
        if (qInvocationSessionInstance == null) {
            return;
        }
        var widget = qInvocationSessionInstance.getViewer().getTextWidget();

        int x;
        int y;
        int invocationOffset = qInvocationSessionInstance.getInvocationOffset();
        int invocationLine = widget.getLineAtOffset(invocationOffset);
        int lineHt = widget.getLineHeight();
        int fontHt = gc.getFontMetrics().getHeight();
        y = (invocationLine + lineInSuggestion + 1) * lineHt - fontHt;
        x = isMacOS ? gc.textExtent(text).x : (int) measureLayout.getBounds().width;
        if (lineInSuggestion == 0) {
            x += widget.getLocationAtOffset(invocationOffset).x;
        }
        int scrollOffsetY = widget.getTopPixel();
        y -= scrollOffsetY;
        String textToRender = String.valueOf(symbol);
        if (currentCaretOffset > openBracket.getRelevantOffset()) {
            Color typedColor = widget.getForeground();
            if (isMacOS) {
                gc.setForeground(typedColor);
                gc.setFont(adjustedTypedFont);
                gc.drawText(textToRender, x, y, false);
            } else {
                layout.setFont(adjustedTypedFont);
                layout.setText(textToRender);
                layout.setTabs(widget.getTabStops());
                gc.setAlpha(255);
                layout.draw(gc, x, y);
            }
        } else {
            if (isMacOS) {
                gc.setForeground(Q_INLINE_HINT_TEXT_COLOR);
                gc.setFont(qInvocationSessionInstance.getInlineTextFont());
                gc.drawText(textToRender, x, y, true);
            } else {
                layout.setFont(qInvocationSessionInstance.getInlineTextFont());
                layout.setText(textToRender);
                layout.setTabs(widget.getTabStops());
                gc.setAlpha(127);
                layout.draw(gc, x, y);
            }
        }
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
    public String getAutoCloseContent(final boolean isBracketSetToAutoClose,
            final boolean isAngleBracketsSetToAutoClose, final boolean isBracesSetToAutoClose,
            final boolean isStringSetToAutoClose) {
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

    public QInlineSuggestionOpenBracketSegment getOpenBracket() {
        return openBracket;
    }

    @Override
    public void cleanUp() {
        if (layout != null) {
            layout.dispose();
        }
        if (measureLayout != null) {
            measureLayout.dispose();
        }
    }
}
