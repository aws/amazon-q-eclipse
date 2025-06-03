// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

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
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.ITextEditor;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

public final class QInlineInputListener implements IDocumentListener, VerifyKeyListener, MouseListener {
    private StyledText widget = null;
    private int numSuggestionLines = 0;
    private List<IQInlineSuggestionSegment> suggestionSegments = new ArrayList<>();
    private IQInlineBracket[] brackets;
    private int distanceTraversed = 0;
    private int normalSegmentCount = 0;
    private String rightCtxBuf = "";
    private IQInlineTypeaheadProcessor typeaheadProcessor;

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
        ITextEditor editor = session.getEditor();
        typeaheadProcessor = QEclipseEditorUtils.getAutoCloseSettings(editor);
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
        QInvocationSession session = QInvocationSession.getInstance();
        // We want to modify the document prior to attaching document listener
        // For that reason, we should move this document listener to onNewSuggestion.
        // - Check to see if the right context exists.
        // - If it does, check to see if the suggestion spans more than 1 line.
        // - If it does, save the right context on the same line.
        // - Add in a segment to render the strike through right context.
        // - Delete the right context (excluding the new line).
        ITextViewer viewer = session.getViewer();
        IDocument doc = viewer.getDocument();
        doc.removeDocumentListener(this);
        int invocationOffset = session.getInvocationOffset();
        int curLineInDoc = widget.getLineAtOffset(invocationOffset);
        int lineIdx = invocationOffset - widget.getOffsetAtLine(curLineInDoc);
        String contentInLine = widget.getLine(curLineInDoc);
        if (!rightCtxBuf.isEmpty() && normalSegmentCount > 1) {
            try {
                int adjustedOffset = QEclipseEditorUtils.getOffsetInFullyExpandedDocument(session.getViewer(),
                        session.getInvocationOffset());
                doc.replace(adjustedOffset, 0, rightCtxBuf.split(widget.getLineDelimiter(), 2)[0]);
            } catch (BadLocationException e) {
                Activator.getLogger().error(e.toString());
            }
        }
        if (!suggestionSegments.isEmpty()) {
            suggestionSegments.clear();
        }
        numSuggestionLines = session.getCurrentSuggestion().getInsertText().split("\\R").length;
        List<IQInlineSuggestionSegment> segments = IQInlineSuggestionSegmentFactory.getSegmentsFromSuggestion(session);
        brackets = new IQInlineBracket[session.getCurrentSuggestion().getInsertText().length()];
        if (lineIdx < contentInLine.length()) {
            rightCtxBuf = contentInLine.substring(lineIdx);
        }
        int normalSegmentNum = 0;
        for (var segment : segments) {
            if (segment instanceof IQInlineBracket) {
                int offset = ((IQInlineBracket) segment).getRelevantOffset();
                int idxInSuggestion = offset - invocationOffset;
                if (((IQInlineBracket) segment).getSymbol() == '{'
                        && typeaheadProcessor.isCurlyBracesAutoCloseDelayed()) {
                    int firstNewLineAfter = session.getCurrentSuggestion().getInsertText().indexOf('\n',
                            idxInSuggestion);
                    if (firstNewLineAfter != -1) {
                        brackets[firstNewLineAfter] = (IQInlineBracket) segment;
                    }
                } else {
                    brackets[idxInSuggestion] = (IQInlineBracket) segment;
                }
                // We only add close brackets to be rendered separately
                if (segment instanceof QInlineSuggestionCloseBracketSegment) {
                    suggestionSegments.add(segment);
                }
            } else {
                suggestionSegments.add(segment);
                normalSegmentNum++;
            }
        }
        if (normalSegmentNum > 1 && !rightCtxBuf.isEmpty()) {
            QInlineSuggestionRightContextSegment rightCtxSegment = IQInlineSuggestionSegmentFactory
                    .getRightCtxSegment(rightCtxBuf, session.getCurrentSuggestion().getInsertText().split("\\R", 2)[0]);
            suggestionSegments.add(rightCtxSegment);
            try {
                int expandedOffset = QEclipseEditorUtils.getOffsetInFullyExpandedDocument(viewer, invocationOffset);
                // We want to leave the '\n' on the current line
                int rightCtxEffectiveLength = rightCtxBuf.endsWith("\n") ? rightCtxBuf.length() - 1
                        : rightCtxBuf.length();
                doc.replace(expandedOffset, rightCtxEffectiveLength, "");
            } catch (BadLocationException e) {
                Activator.getLogger().error("Error striking out document right context" + e.toString());
            }
        }
        normalSegmentCount = normalSegmentNum;
        doc.addDocumentListener(this);
    }

    public List<IQInlineSuggestionSegment> getSegments() {
        return suggestionSegments;
    }

    public int getOutstandingPadding() {
        return typeaheadProcessor.getOutstandingPadding(brackets);
    }

    /**
     * Here we need to perform the following before the listener gets removed:
     * <ul>
     * <li>If the auto closing of brackets was enabled originally, we should add
     * these closed brackets back into the buffer.</li>
     * <li>Revert the settings back to their original states.</li>
     * </ul>
     */
    public void beforeRemoval() {
        var session = QInvocationSession.getInstance();
        IDocument doc = session.getViewer().getDocument();
        doc.removeDocumentListener(this);
        if (session == null || !session.isActive() || brackets == null || session.getSuggestionAccepted()) {
            return;
        }

        String toAppend = "";
        for (int i = brackets.length - 1; i >= 0; i--) {
            var bracket = brackets[i];
            if (bracket == null) {
                continue;
            }
            boolean isBracketsSetToAutoClose = typeaheadProcessor.isBracesSetToAutoClose();
            boolean isAngleBracketsSetToAutoClose = typeaheadProcessor.isAngleBracketsSetToAutoClose();
            boolean isBracesSetToAutoClose = typeaheadProcessor.isBracesSetToAutoClose();
            boolean isStringSetToAutoClose = typeaheadProcessor.isStringSetToAutoClose();
            String autoCloseContent = bracket.getAutoCloseContent(isBracketsSetToAutoClose,
                    isAngleBracketsSetToAutoClose, isBracesSetToAutoClose, isStringSetToAutoClose);
            if (autoCloseContent != null) {
                toAppend += autoCloseContent;
            }
        }
        toAppend += rightCtxBuf;
        suggestionSegments.stream().forEach((segment) -> segment.cleanUp());
        final String toAppendFinal = toAppend;
        int idx = distanceTraversed;
        if (!toAppend.isEmpty()) {
                try {
                    int currentOffset = session.getInvocationOffset() + idx;
                    int expandedCurrentOffset = QEclipseEditorUtils.getOffsetInFullyExpandedDocument(session.getViewer(),
                            currentOffset);
                    int lineNumber = doc.getLineOfOffset(expandedCurrentOffset);
                    int startLineOffset = doc.getLineOffset(lineNumber);
                    int curLineInDoc = widget.getLineAtOffset(currentOffset);
                    int lineIdx = expandedCurrentOffset - startLineOffset;
                    String contentInLine = widget.getLine(curLineInDoc);
                    String currentRightCtx = contentInLine.substring(lineIdx);
                    int distanceToNewLine = currentRightCtx.length();
                    Display.getCurrent().asyncExec(() -> {
                        try {
                            doc.replace(expandedCurrentOffset, distanceToNewLine, toAppendFinal);
                        } catch (BadLocationException e) {
                            Activator.getLogger().error("Error appending right context", e);
                        }
                    });
                } catch (BadLocationException e) {
                    Activator.getLogger().error("Error retrieving line information for appending right context", e);
                }
        }
    }

    @Override
    public void verifyKey(final VerifyEvent event) {
        var session = QInvocationSession.getInstance();
        if (session == null || !session.isPreviewingSuggestions()) {
            return;
        }

        // We need to provide the reason for the caret movement. This way we can perform
        // subsequent actions accordingly:
        // - If the caret has been moved due to traversals (i.e. arrow keys or mouse
        // click) we would want to end the invocation session since that signifies the
        // user no longer has the intent for text input at its original location.
        if (event.keyCode == SWT.ARROW_UP || event.keyCode == SWT.ARROW_DOWN || event.keyCode == SWT.ARROW_LEFT
                || event.keyCode == SWT.ARROW_RIGHT) {
            session.setCaretMovementReason(CaretMovementReason.MOVEMENT_KEY);
            return;
        }

        session.setCaretMovementReason(CaretMovementReason.TEXT_INPUT);

        if (event.keyCode == SWT.BS && distanceTraversed == 0) {
            session.transitionToDecisionMade();
            session.end();
            return;
        }
        // Here we want to check for the following:
        // - If the input is a closing bracket
        // - If it is, does it correspond to the most recent open bracket in the
        // unresolved bracket list
        // - If it does, we need to perform the following:
        // - Adjust the caret offset backwards by one. This is because the caret offset
        // in documentChanged is the offset before the change is made. Since this key
        // event will not actually trigger documentChanged under the right condition
        // (and what ends up triggering documentChanged is the doc.replace call), we
        // will need to decrement the offset to prepare for when the documentChanged is
        // triggered.
        // - Insert the closing bracket into the buffer at the decremented position.
        // This is because eclipse will not actually insert closing bracket when auto
        // close is turned on.
        TypeaheadProcessorInstruction instruction = typeaheadProcessor.processVerifyKeyBuffer(distanceTraversed,
                event.character, brackets);
        if (instruction.shouldModifyCaretOffset()) {
            widget.setCaretOffset(instruction.getCaretOffset());
        }
        if (instruction.shouldModifyDocument()) {
            try {
                ITextViewer viewer = session.getViewer();
                IDocument doc = viewer.getDocument();
                int expandedOffset = QEclipseEditorUtils.getOffsetInFullyExpandedDocument(session.getViewer(),
                        instruction.getDocInsertOffset());
                doc.replace(expandedOffset, instruction.getDocInsertLength(), instruction.getDocInsertContent());
            } catch (BadLocationException e) {
                Activator.getLogger().error("Error inserting close bracket during typeahead", e);
            }
        }
    }

    @Override
    public void documentChanged(final DocumentEvent event) {
        QInvocationSession session = QInvocationSession.getInstance();

        if (session == null || !session.isPreviewingSuggestions()) {
            return;
        }

        String input = event.getText();
        if (input.equals("( ") || input.equals("[ ") || input.equals("< ") || input.equals("\" ")
                || input.equals("\' ")) {
            input = input.substring(0, 1);
        }
        String currentSuggestion = session.getCurrentSuggestion().getInsertText();
        int currentOffset = widget.getCaretOffset();

        if (input.isEmpty()) {
            if (distanceTraversed <= 0) {
                // discard all suggestions as caret position is less than request invocation position
                session.updateCompletionStates(new ArrayList<String>());
                session.transitionToDecisionMade();
                session.end();
                return;
            }
            distanceTraversed = typeaheadProcessor.getNewDistanceTraversedOnDeleteAndUpdateBracketState(
                    event.getLength(), distanceTraversed, brackets);
            if (distanceTraversed < 0) {
                // discard all suggestions as caret position is less than request invocation position
                session.updateCompletionStates(new ArrayList<String>());
                session.transitionToDecisionMade();
                session.end();
            }

            // note: distanceTraversed as 0 is currently understood to be when a user presses BS removing any typeahead
            if (distanceTraversed == 0) {
                // reset completion states for all suggestions when user reverts to request
                // invocation state
                session.resetCompletionStates();
                // mark currently displayed suggestion as seen
                session.markSuggestionAsSeen();
            }

            return;
        }

        // Here we perform "pre-open bracket insertion input sanitation", which consists
        // of the following:
        // - Checks to see if the input contains anything inserted on behalf of user by
        // eclipse (i.e. auto closing bracket).
        // - If it does, get rid of that portion (note that at this point the document
        // has already been changed so we are really deleting the extra portion and
        // replacing it with what should remain).
        // - Lastly, we would want to return early for these two cases. This is because
        // the very act of altering the document will trigger this callback once again
        // so there is no need to validate the input this time around.
        TypeaheadProcessorInstruction preprocessInstruction = typeaheadProcessor
                .preprocessDocumentChangedBuffer(distanceTraversed, currentOffset, input, brackets);
        if (preprocessInstruction.shouldModifyCaretOffset()) {
            widget.setCaretOffset(preprocessInstruction.getCaretOffset());
        }
        if (preprocessInstruction.shouldModifyDocument()) {
            try {
                int expandedOffset = QEclipseEditorUtils.getOffsetInFullyExpandedDocument(session.getViewer(),
                        preprocessInstruction.getDocInsertOffset());
                event.getDocument().replace(expandedOffset, preprocessInstruction.getDocInsertLength(),
                        preprocessInstruction.getDocInsertContent());
                return;
            } catch (BadLocationException e) {
                Activator.getLogger().error("Error performing open bracket sanitation during typeahead", e);
            }
        }

        boolean isOutOfBounds = distanceTraversed + input.length() >= currentSuggestion.length()
                || distanceTraversed < 0;
        if (isOutOfBounds || !isInputAMatch(currentSuggestion, distanceTraversed, input)) {
            distanceTraversed += input.length();
            event.getDocument().removeDocumentListener(this);
            StyledText widget = session.getViewer().getTextWidget();
            int caretLine = widget.getLineAtOffset(widget.getCaretOffset());
            int linesOfInput = input.split(widget.getLineDelimiter()).length;
            int lineToUnsetIndent = caretLine + linesOfInput;
            session.transitionToDecisionMade(lineToUnsetIndent);
            Display.getCurrent().asyncExec(() -> {
                if (session.isActive()) {
                    // discard suggestions and end immediately as typeahead does not match
                    session.updateCompletionStates(new ArrayList<String>());
                    session.endImmediately();
                }
            });
            return;
        }

        // discard all other suggestions except for current one as typeahead matches it
        // also mark current one as seen as it continues to be displayed
        session.updateCompletionStates(List.of(session.getCurrentSuggestion().getItemId()));
        session.markSuggestionAsSeen();

        // Here we perform "post closing bracket insertion caret correction", which
        // consists of the following:
        // - Check if the input is a closing bracket
        // - If it is, check to see if it corresponds to the most recent unresolved open
        // bracket
        // - If it is, we would need to increment the current caret offset, this is
        // because the closing bracket would have been inserted by verifyKey and not
        // organically, which does not advance the caret.
        TypeaheadProcessorInstruction postProcessInstruction = typeaheadProcessor
                .postProcessDocumentChangeBuffer(distanceTraversed, currentOffset, input, brackets);
        if (postProcessInstruction.shouldModifyCaretOffset()) {
            int targetOffset = postProcessInstruction.getCaretOffset();
            widget.setCaretOffset(targetOffset);
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
    }

    private boolean isInputAMatch(final String currentSuggestion, final int startIdx, final String input) {
        boolean res = false;
        if (input.length() > 1 && input.length() + startIdx <= currentSuggestion.length()) {
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
        int invocationOffset = qInvocationSessionInstance.getInvocationOffset();
        int currentOffset = invocationOffset + distanceTraversed;
        int lastKnownLine = widget.getLineAtOffset(currentOffset);
        qInvocationSessionInstance.transitionToDecisionMade(lastKnownLine + 1);
        qInvocationSessionInstance.endImmediately();
        return;
    }

    @Override
    public void mouseUp(final MouseEvent e) {
        return;
    }
}
