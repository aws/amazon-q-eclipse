// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.util;

import static software.aws.toolkits.eclipse.amazonq.util.QConstants.Q_INLINE_HINT_TEXT_COLOR;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

public class QInlineSuggestionNormalSegment implements IQInlineSuggestionSegment {
	private int startCaretOffset;
	private int endCaretOffset;
	private int lineInSuggestion;
	private String text;

	public QInlineSuggestionNormalSegment(int startCaretPosition, int endCaretPosition, int lineInSuggestion,
			String text) {
		this.text = text;
		this.startCaretOffset = startCaretPosition;
		this.endCaretOffset = endCaretPosition;
		this.lineInSuggestion = lineInSuggestion;
	}

	@Override
	public void render(GC gc, int currentCaretOffset) {
		if (currentCaretOffset > endCaretOffset) {
			return;
		}
		var qInvocationSessionInstance = QInvocationSession.getInstance();
		if (qInvocationSessionInstance == null) {
			return;
		}
		var widget = qInvocationSessionInstance.getViewer().getTextWidget();

		int x, y;
		String textToRender;
		Point location = widget.getLocationAtOffset(currentCaretOffset);
		int invocationLine = widget.getLineAtOffset(qInvocationSessionInstance.getInvocationOffset());
		int lineHt = widget.getLineHeight();
		int fontHt = gc.getFontMetrics().getHeight();
		y = (invocationLine + lineInSuggestion + 1) * lineHt - fontHt;

		if (currentCaretOffset < startCaretOffset) {
			x = widget.getLeftMargin();
			textToRender = text;
		} else {
			x = location.x;
			textToRender = text.substring(currentCaretOffset - startCaretOffset);
		}

		gc.setForeground(Q_INLINE_HINT_TEXT_COLOR);
		gc.setFont(qInvocationSessionInstance.getInlineTextFont());
		gc.drawText(textToRender, x, y, true);
	}
}
