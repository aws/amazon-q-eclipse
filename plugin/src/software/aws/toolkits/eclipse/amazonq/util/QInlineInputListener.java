// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

public final class QInlineInputListener implements IDocumentListener, VerifyKeyListener, MouseListener {

    private StyledText widget = null;
    private int distanceTraversed = 0;
    private int numSuggestionLines = 0;
    private LastKeyStrokeType lastKeyStrokeType = LastKeyStrokeType.NORMAL_INPUT;
    private boolean isBracketsSetToAutoClose = false;
    private boolean isBracesSetToAutoClose = false;
    private boolean isStringSetToAutoClose = false;
    private List<IQInlineSuggestionSegment> suggestionSegments = new ArrayList<>();
    private IQInlineBracket[] brackets;

    private enum LastKeyStrokeType {
        NORMAL_INPUT, BACKSPACE,
    }

    /**
     * During instantiation we would need to perform the following to prime the
     * listeners for typeahead:
     * <ul>
     * <li>Set these auto closing settings to false.</li>
     * <li>Analyze the buffer in current suggestions for bracket pairs.</li>
     * </ul>
     *
     * @param widget
     */
    public QInlineInputListener(final StyledText widget) {
        this.widget = widget;
        QInvocationSession session = QInvocationSession.getInstance();
        ITextViewer viewer = session.getViewer();
        IDocument doc = viewer.getDocument();
        doc.addDocumentListener(this);
    }

    /**
     * A routine to prime the class for typeahead related information. These are:
     * <ul>
     * <li>Where each bracket pairs are.</li>
     * </ul>
     *
     * This is to be called on instantiation as well as when new suggestion has been
     * toggled to.
     */
    public void onNewSuggestion() {
        lastKeyStrokeType = LastKeyStrokeType.NORMAL_INPUT;
        var qInvocationSessionInstance = QInvocationSession.getInstance();
        if (qInvocationSessionInstance == null) {
            return;
        }
        if (!suggestionSegments.isEmpty()) {
            suggestionSegments.clear();
        }
        numSuggestionLines = qInvocationSessionInstance.getCurrentSuggestion().getInsertText().split("\\R").length;
        List<IQInlineSuggestionSegment> segments = IQInlineSuggestionSegmentFactory
                .getSegmentsFromSuggestion(qInvocationSessionInstance);
        brackets = new IQInlineBracket[qInvocationSessionInstance.getCurrentSuggestion().getInsertText().length()];
        int invocationOffset = qInvocationSessionInstance.getInvocationOffset();
        for (var segment : segments) {
            if (segment instanceof IQInlineBracket) {
                int offset = ((IQInlineBracket) segment).getRelevantOffset();
                int idxInSuggestion = offset - invocationOffset;
                if (((IQInlineBracket) segment).getSymbol() == '{') {
                    int firstNewLineAfter = qInvocationSessionInstance.getCurrentSuggestion().getInsertText()
                            .indexOf('\n', idxInSuggestion);
                    brackets[firstNewLineAfter] = (IQInlineBracket) segment;
                } else {
                    brackets[idxInSuggestion] = (IQInlineBracket) segment;
                }
                // We only add close brackets to be rendered separately
                if (segment instanceof QInlineSuggestionCloseBracketSegment) {
                    suggestionSegments.add(segment);
                }
            } else {
                suggestionSegments.add(segment);
            }
        }
    }

    public List<IQInlineSuggestionSegment> getSegments() {
        return suggestionSegments;
    }

    /**
     * Here we need to perform the following before the listener gets removed:
     * <ul>
     * <li>If the auto closing of brackets was enabled originally, we should add these closed brackets back into the buffer.</li>
     * <li>Revert the settings back to their original states.</li>
     * </ul>
     */
    public void beforeRemoval() {
        var qSes = QInvocationSession.getInstance();
        if (qSes == null || !qSes.isActive() || brackets == null) {
            return;
        }

        String toAppend = "";
        for (int i = brackets.length - 1; i >= 0; i--) {
            var bracket = brackets[i];
            if (bracket == null) {
                continue;
            }
            if (!qSes.getSuggestionAccepted()) {
                String autoCloseContent = bracket.getAutoCloseContent(isBracketsSetToAutoClose, isBracesSetToAutoClose,
                        isStringSetToAutoClose);
                if (autoCloseContent != null) {
                    toAppend += autoCloseContent;
                }
            }
        }

        IDocument doc = qSes.getViewer().getDocument();
        doc.removeDocumentListener(this);
        if (!toAppend.isEmpty()) {
            try {
                int adjustedOffset = QEclipseEditorUtils.getOffsetInFullyExpandedDocument(qSes.getViewer(),
                        qSes.getInvocationOffset()) + distanceTraversed;
                doc.replace(adjustedOffset, 0, toAppend);
            } catch (BadLocationException e) {
                Activator.getLogger().error(e.toString());
            }
        }
    }

    @Override
    public void verifyKey(final VerifyEvent event) {
        var qInvocationSessionInstance = QInvocationSession.getInstance();
        if (qInvocationSessionInstance == null || !qInvocationSessionInstance.isPreviewingSuggestions()) {
            return;
        }

        // We need to provide the reason for the caret movement. This way we can perform
        // subsequent actions accordingly:
        // - If the caret has been moved due to traversals (i.e. arrow keys or mouse
        // click) we would want to end the invocation session since that signifies the
        // user no longer has the intent for text input at its original location.
        if (event.keyCode == SWT.ARROW_UP || event.keyCode == SWT.ARROW_DOWN || event.keyCode == SWT.ARROW_LEFT
                || event.keyCode == SWT.ARROW_RIGHT) {
            qInvocationSessionInstance.setCaretMovementReason(CaretMovementReason.MOVEMENT_KEY);
            return;
        }

        qInvocationSessionInstance.setCaretMovementReason(CaretMovementReason.TEXT_INPUT);

        // Here we examine all other relevant keystrokes that may be relevant to the
        // preview's lifetime:
        // - CR (new line)
        // - BS (backspace)
        switch (event.keyCode) {
        case SWT.CR:
            lastKeyStrokeType = LastKeyStrokeType.NORMAL_INPUT;
            return;
        case SWT.BS:
            if (distanceTraversed == 0) {
                qInvocationSessionInstance.transitionToDecisionMade();
                qInvocationSessionInstance.end();
                return;
            }
            lastKeyStrokeType = LastKeyStrokeType.BACKSPACE;
            return;
        default:
            lastKeyStrokeType = LastKeyStrokeType.NORMAL_INPUT;
            return;
        }
    }

    @Override
    public void documentChanged(final DocumentEvent event) {
        String input = event.getText();
        if (input.isEmpty()) {
            var qInvocationSessionInstance = QInvocationSession.getInstance();
            int numCharDeleted = event.getLength();
            if (numCharDeleted > distanceTraversed) {
                qInvocationSessionInstance.transitionToDecisionMade();
                qInvocationSessionInstance.end();
            }
            for (int i = 1; i <= numCharDeleted; i++) {
                var bracket = brackets[distanceTraversed - i];
                if (bracket != null) {
                    bracket.onDelete();
                }
            }
            distanceTraversed -= numCharDeleted;
            return;
        }

        var qInvocationSessionInstance = QInvocationSession.getInstance();
        if (qInvocationSessionInstance == null || !qInvocationSessionInstance.isPreviewingSuggestions()) {
            return;
        }

        if (input.length() > 1 && (input.equals("()") || input.equals("{}") || input.equals("<>")
                || input.equals("\"\"") || input.equals("[]"))) {
            String openBracket = input.substring(0, 1);
            ITextViewer viewer = qInvocationSessionInstance.getViewer();
            IDocument doc = viewer.getDocument();
            int expandedOffset = QEclipseEditorUtils.getOffsetInFullyExpandedDocument(viewer, event.getOffset());
            try {
                doc.replace(expandedOffset, event.getLength() + 2, openBracket);
            } catch (BadLocationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        String currentSuggestion = qInvocationSessionInstance.getCurrentSuggestion().getInsertText();
        int currentOffset = widget.getCaretOffset();
        qInvocationSessionInstance
                .setHasBeenTypedahead(currentOffset - qInvocationSessionInstance.getInvocationOffset() > 0);

        boolean isOutOfBounds = distanceTraversed >= currentSuggestion.length() || distanceTraversed < 0;
        if (isOutOfBounds || !isInputAMatch(currentSuggestion, distanceTraversed, input)) {
            System.out.println("input is: "
                    + input.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t").replace(' ', 's'));
            System.out.println("suggestion is: "
                    + currentSuggestion.substring(distanceTraversed, distanceTraversed + input.length())
                            .replace("\n", "\\n").replace("\r", "\\r".replace("\t", "\\t").replace(' ', 's')));
            Display.getCurrent().asyncExec(() -> {
                qInvocationSessionInstance.transitionToDecisionMade();
                qInvocationSessionInstance.end();
            });
            return;
        }
        for (int i = distanceTraversed; i < distanceTraversed + input.length(); i++) {
            var bracket = brackets[i];
            if (bracket != null) {
                bracket.onTypeOver();
            }
        }
        distanceTraversed += input.length();
    }

    @Override
    public void documentAboutToBeChanged(final DocumentEvent e) {
        // TODO Auto-generated method stub
    }

    private boolean isInputAMatch(final String currentSuggestion, final int startIdx, final String input) {
        boolean res = false;
        if (input.length() > 1 && input.length() <= currentSuggestion.length()) {
            res = currentSuggestion.substring(startIdx, startIdx + input.length()).equals(input);
        } else if (input.length() == 1) {
            res = String.valueOf(currentSuggestion.charAt(startIdx)).equals(input);
        }
        return res;
    }

    public int getNumSuggestionLines() {
        return numSuggestionLines;
    }

    @Override
    public void mouseDoubleClick(final MouseEvent e) {
        return;
    }

    @Override
    public void mouseDown(final MouseEvent e) {
        // For the most part setting status here is pointless (for now)
        // This is because the only other component that is relying on
        // CaretMovementReason
        // (the CaretListener) is called _before_ the mouse listener
        // For consistency sake, we'll stick with updating it now.
        var qInvocationSessionInstance = QInvocationSession.getInstance();
        if (!qInvocationSessionInstance.isActive()) {
            return;
        }
        qInvocationSessionInstance.setCaretMovementReason(CaretMovementReason.MOUSE);
        int lastKnownLine = qInvocationSessionInstance.getLastKnownLine();
        qInvocationSessionInstance.transitionToDecisionMade(lastKnownLine + 1);
        qInvocationSessionInstance.end();
        return;
    }

    @Override
    public void mouseUp(final MouseEvent e) {
        return;
    }
}
