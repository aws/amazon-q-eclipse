// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.texteditor.ITextEditor;

public final class JavaTypeaheadProcessor implements IQInlineTypeaheadProcessor {
    private static final Pattern CURLY_AUTO_CLOSE_MATCHER = Pattern.compile("\\n[ \\t]*\\n\\s*\\}");

    private StyledText widget;
    private ITextViewer viewer;

    private boolean isBracesSetToAutoClose = true;
    private boolean isBracketsSetToAutoClose = true;
    private boolean isStringSetToAutoClose = true;

    public JavaTypeaheadProcessor(final ITextEditor editor, final boolean isBracesSetToAutoClose,
            final boolean isBracketsSetToAutoClose, final boolean isStringSetToAutoClose) {
        viewer = (ITextViewer) editor.getAdapter(ITextViewer.class);
        widget = viewer.getTextWidget();
        this.isBracesSetToAutoClose = isBracesSetToAutoClose;
        this.isBracketsSetToAutoClose = isBracketsSetToAutoClose;
        this.isStringSetToAutoClose = isStringSetToAutoClose;
    }

    @Override
    public int getNewDistanceTraversedOnDeleteAndUpdateBracketState(final int inputLength,
            final int currentDistanceTraversed, final IQInlineBracket[] brackets) {
        int numCharDeleted = inputLength;
        int paddingLength = 0;
        for (int i = 1; i <= numCharDeleted; i++) {
            var bracket = brackets[currentDistanceTraversed - i];
            if (bracket != null) {
                if ((bracket instanceof QInlineSuggestionOpenBracketSegment)
                        && !((QInlineSuggestionOpenBracketSegment) bracket).isResolved()) {
                    paddingLength++;
                }
                bracket.onDelete();
            }
        }
        int distanceTraversed = currentDistanceTraversed - (numCharDeleted - paddingLength);
        return distanceTraversed;
    }

    @Override
    public TypeaheadProcessorInstruction preprocessDocumentChangedBuffer(final int distanceTraversed,
            final int eventOffset, final String input, final IQInlineBracket[] brackets) {
        TypeaheadProcessorInstruction res = new TypeaheadProcessorInstruction();
        PreprocessingCategory category = getBufferPreprocessingCategory(distanceTraversed, input, brackets);
        switch (category) {
        case STR_QUOTE_OPEN:
        case NORMAL_BRACKETS_OPEN:
            res.setShouldModifyDocument(true);
            res.setDocInsertOffset(eventOffset);
            res.setDocInsertLength(2);
            res.setDocInsertContent(input.substring(0, 1) + " ");
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
    public TypeaheadProcessorInstruction postProcessDocumentChangeBuffer(final int distanceTraversed,
            final int currentOffset, final String input, final IQInlineBracket[] brackets) {
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
        if (openBracket == null || openBracket.isResolved()) {
            return res;
        }
        switch (input.charAt(0)) {
        case ')':
        case ']':
        case '>':
            if (isBracketsSetToAutoClose) {
                res.setShouldModifyCaretOffset(true);
                res.setCaretOffset(currentOffset + 1);
            }
            break;
        case '\"':
        case '\'':
            if (isStringSetToAutoClose) {
                res.setShouldModifyCaretOffset(true);
                res.setCaretOffset(currentOffset + 1);
            }
            break;
        default:
            break;
        }

        return res;
    }

    @Override
    public TypeaheadProcessorInstruction processVerifyKeyBuffer(final int distanceTraversed, final char input,
            final IQInlineBracket[] brackets) {
        TypeaheadProcessorInstruction res = new TypeaheadProcessorInstruction();
        if (shouldProcessVerifyKeyInput(input, distanceTraversed, brackets)) {
            int expandedOffset = QEclipseEditorUtils.getOffsetInFullyExpandedDocument(viewer, widget.getCaretOffset());
            res.setShouldModifyCaretOffset(true);
            res.setShouldModifyDocument(true);
            res.setCaretOffset(widget.getCaretOffset() - 1);
            res.setDocInsertOffset(expandedOffset - 1);
            res.setDocInsertLength(0);
            res.setDocInsertContent(String.valueOf(input));
        } else {
            res.setShouldModifyCaretOffset(false);
            res.setShouldModifyDocument(false);
        }
        return res;
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
        switch (input) {
        case ')':
        case ']':
        case '>':
            if (!isBracketsSetToAutoClose) {
                return false;
            }
            break;
        case '\"':
        case '\'':
            if (!isStringSetToAutoClose) {
                return false;
            }
            break;
        default:
            break;
        }
        QInlineSuggestionOpenBracketSegment openBracket = ((QInlineSuggestionCloseBracketSegment) bracket)
                .getOpenBracket();
        if (openBracket == null || openBracket.isResolved()) {
            return false;
        }
        return true;
    }

    private PreprocessingCategory getBufferPreprocessingCategory(final int distanceTraversed, final String input,
            final IQInlineBracket[] brackets) {
        var bracket = brackets[distanceTraversed];
        if (input.length() > 1 && bracket != null && bracket.getSymbol() == input.charAt(0)
                && (input.equals("()") || input.equals("<>") || input.equals("[]"))) {
            return PreprocessingCategory.NORMAL_BRACKETS_OPEN;
        }
        if (input.equals("\"\"") || input.equals("\'\'")) {
            if (bracket != null && bracket.getSymbol() == input.charAt(0)) {
                if (bracket instanceof QInlineSuggestionOpenBracketSegment) {
                    return PreprocessingCategory.STR_QUOTE_OPEN;
                } else {
                    return PreprocessingCategory.STR_QUOTE_CLOSE;
                }
            }
        }
        Matcher matcher = CURLY_AUTO_CLOSE_MATCHER.matcher(input);
        if (matcher.find()) {
            return PreprocessingCategory.CURLY_BRACES;
        }
        if (bracket != null) {
            if ((bracket instanceof QInlineSuggestionCloseBracketSegment) && input.charAt(0) == bracket.getSymbol()
                    && !((QInlineSuggestionCloseBracketSegment) bracket).getOpenBracket().isResolved()) {
                boolean autoCloseEnabled = false;
                switch (bracket.getSymbol()) {
                case '\"':
                case '\'':
                    autoCloseEnabled = isStringSetToAutoClose;
                    break;
                case '>':
                case ')':
                case ']':
                    autoCloseEnabled = isBracketsSetToAutoClose;
                    break;
                default:
                    break;
                }
                if (autoCloseEnabled) {
                    return PreprocessingCategory.NORMAL_BRACKETS_CLOSE;
                }
            }
        }
        return PreprocessingCategory.NONE;
    }

    @Override
    public boolean isBracketsSetToAutoClose() {
        return isBracketsSetToAutoClose;
    }

    @Override
    public boolean isAngleBracketsSetToAutoClose() {
        return isBracketsSetToAutoClose;
    }

    @Override
    public boolean isBracesSetToAutoClose() {
        return isBracesSetToAutoClose;
    }

    @Override
    public boolean isStringSetToAutoClose() {
        return isStringSetToAutoClose;
    }
}
