package software.aws.toolkits.eclipse.amazonq.util;

import java.util.regex.Matcher;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.texteditor.ITextEditor;

public class GenericTypeheadProcessor implements IQInlineTypeaheadProcessor {

    private StyledText widget;
    private ITextViewer viewer;

    public GenericTypeheadProcessor(final ITextEditor editor) {
        viewer = (ITextViewer) editor.getAdapter(ITextViewer.class);
        widget = viewer.getTextWidget();
    }

    @Override
    public int getNewDistanceTraversedOnDeleteAndUpdateBracketState(int inputLength, int currentDistanceTraversed,
            IQInlineBracket[] brackets) {
        for (int i = 1; i <= inputLength; i++) {
            var bracket = brackets[currentDistanceTraversed - i];
            if (bracket != null) {
                bracket.onDelete();
            }
        }
        int distanceTraversed = currentDistanceTraversed - inputLength;
        return distanceTraversed;
    }

    @Override
    public TypeaheadProcessorInstruction preprocessDocumentChangedBuffer(int distanceTraversed, int eventOffset,
            String input, IQInlineBracket[] brackets) {
        TypeaheadProcessorInstruction res = new TypeaheadProcessorInstruction();
        PreprocessingCategory category = getBufferPreprocessingCategory(distanceTraversed, input, brackets);
        switch (category) {
        case STR_QUOTE_OPEN:
        case NORMAL_BRACKETS_OPEN:
            res.setShouldModifyDocument(true);
            res.setDocInsertOffset(eventOffset);
            res.setDocInsertLength(2);
            res.setDocInsertContent(input.substring(0, 1));
            break;
        case NORMAL_BRACKETS_CLOSE:
            brackets[distanceTraversed].onTypeOver();
            res.setShouldModifyCaretOffset(true);
            res.setShouldModifyDocument(true);
            res.setDocInsertOffset(eventOffset);
            res.setDocInsertLength(2);
            res.setDocInsertContent(input);
            res.setCaretOffset(widget.getCaretOffset() + 1);
            break;
        case STR_QUOTE_CLOSE:
            res.setShouldModifyDocument(true);
            res.setDocInsertOffset(eventOffset);
            res.setDocInsertLength(2);
            res.setDocInsertContent(input.substring(0, 1));
            break;
        case CURLY_BRACES:
            int firstNewlineIndex = input.indexOf('\n');
            int secondNewlineIndex = input.indexOf('\n', firstNewlineIndex + 1);
            if (secondNewlineIndex != -1) {
                String sanitizedInput = input.substring(0, secondNewlineIndex);
                res.setShouldModifyDocument(true);
                res.setDocInsertOffset(eventOffset);
                res.setDocInsertLength(input.length());
                res.setDocInsertContent(sanitizedInput);
            }
            break;
        default:
            break;
        }
        return res;
    }

    @Override
    public TypeaheadProcessorInstruction postProcessDocumentChangeBuffer(int distanceTraversed, int currentOffset,
            String input, IQInlineBracket[] brackets) {
        IQInlineBracket bracket = brackets[distanceTraversed];
        TypeaheadProcessorInstruction res = new TypeaheadProcessorInstruction();
        if (bracket == null || !(bracket instanceof QInlineSuggestionCloseBracketSegment)) {
            return res;
        }
        if (bracket.getSymbol() != input.charAt(0) || input.length() > 1) {
            return res;
        }
        QInlineSuggestionOpenBracketSegment openBracket = ((QInlineSuggestionCloseBracketSegment) bracket)
                .getOpenBracket();
        if (openBracket == null || openBracket.isResolved() || !openBracket.hasAutoCloseOccurred()) {
            return res;
        }
        res.setShouldModifyCaretOffset(true);
        res.setCaretOffset(currentOffset + 1);
        return res;
    }

    @Override
    public TypeaheadProcessorInstruction processVerifyKeyBuffer(int distanceTraversed, char input,
            IQInlineBracket[] brackets) {
        TypeaheadProcessorInstruction res = new TypeaheadProcessorInstruction();
        if (shouldProcessVerifyKeyInput(input, distanceTraversed, brackets)) {
            int expandedOffset = QEclipseEditorUtils.getOffsetInFullyExpandedDocument(viewer, widget.getCaretOffset());
            res.setShouldModifyCaretOffset(true);
            res.setShouldModifyDocument(true);
            res.setCaretOffset(widget.getCaretOffset() - 1);
            res.setDocInsertOffset(expandedOffset - 1);
            res.setDocInsertLength(0);
            res.setDocInsertContent(String.valueOf(input));
        }
        return res;
    }

    @Override
    public boolean isBracketsSetToAutoClose() {
        return true;
    }

    @Override
    public boolean isAngleBracketsSetToAutoClose() {
        return true;
    }

    @Override
    public boolean isBracesSetToAutoClose() {
        return true;
    }

    @Override
    public boolean isStringSetToAutoClose() {
        return true;
    }

    private boolean shouldProcessVerifyKeyInput(final char input, final int offset, final IQInlineBracket[] brackets) {
        if (brackets[offset] == null) {
            return false;
        }
        IQInlineBracket bracket = brackets[offset];
        if (!(bracket instanceof QInlineSuggestionCloseBracketSegment)) {
            return false;
        }
        if (bracket.getSymbol() != input) {
            return false;
        }
        QInlineSuggestionOpenBracketSegment openBracket = ((QInlineSuggestionCloseBracketSegment) bracket)
                .getOpenBracket();
        if (openBracket == null || openBracket.isResolved() || !openBracket.hasAutoCloseOccurred()) {
            return false;
        }
        return true;
    }

    private PreprocessingCategory getBufferPreprocessingCategory(final int distanceTraversed, final String input,
            final IQInlineBracket[] brackets) {
        var bracket = brackets[distanceTraversed];
        if (input.length() > 1 && bracket != null && bracket.getSymbol() == input.charAt(0)
                && (input.equals("()") || input.equals("<>") || input.equals("[]"))) {
            ((QInlineSuggestionOpenBracketSegment) bracket).setAutoCloseOccurred(true);
            return PreprocessingCategory.NORMAL_BRACKETS_OPEN;
        }
        if (input.equals("\"\"") || input.equals("\'\'")) {
            if (bracket != null && bracket.getSymbol() == input.charAt(0)) {
                if (bracket instanceof QInlineSuggestionOpenBracketSegment) {
                    ((QInlineSuggestionOpenBracketSegment) bracket).setAutoCloseOccurred(true);
                    return PreprocessingCategory.STR_QUOTE_OPEN;
                } else {
                    return PreprocessingCategory.STR_QUOTE_CLOSE;
                }
            }
        }
        Matcher matcher = CURLY_AUTO_CLOSE_MATCHER.matcher(input);
        if (matcher.find()) {
            ((QInlineSuggestionOpenBracketSegment) bracket).setAutoCloseOccurred(true);
            return PreprocessingCategory.CURLY_BRACES;
        }
        if (bracket != null) {
            if ((bracket instanceof QInlineSuggestionCloseBracketSegment) && input.charAt(0) == bracket.getSymbol()
                    && !((QInlineSuggestionCloseBracketSegment) bracket).getOpenBracket().isResolved()
                    && ((QInlineSuggestionCloseBracketSegment) bracket).getOpenBracket().hasAutoCloseOccurred()) {
                return PreprocessingCategory.NORMAL_BRACKETS_CLOSE;
            }
        }
        return PreprocessingCategory.NONE;
    }
}
