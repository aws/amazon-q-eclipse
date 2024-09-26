// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.StyledText;

public final class QInlineCaretListener implements CaretListener {
    private StyledText widget = null;
    private int previousLine = -1;

    public QInlineCaretListener(final StyledText widget) {
        this.widget = widget;
        this.previousLine = widget.getLineAtOffset(widget.getCaretOffset());
    }

    @Override
    public void caretMoved(final CaretEvent event) {
        var qInvocationSessionInstance = QInvocationSession.getInstance();
        CaretMovementReason caretMovementReason = qInvocationSessionInstance.getCaretMovementReason();

        // We want to ignore caret movements induced by text editing
        if (caretMovementReason == CaretMovementReason.TEXT_INPUT) {
            qInvocationSessionInstance.setCaretMovementReason(CaretMovementReason.UNEXAMINED);
            previousLine = widget.getLineAtOffset(widget.getCaretOffset());
            return;
        }

        if (qInvocationSessionInstance.isPreviewingSuggestions()) {
            qInvocationSessionInstance.transitionToDecisionMade(previousLine + 1);
            qInvocationSessionInstance.end();
            return;
        }

        previousLine = widget.getCaretOffset();
    }
}
