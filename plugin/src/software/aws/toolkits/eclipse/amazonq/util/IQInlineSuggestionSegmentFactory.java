// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public final class IQInlineSuggestionSegmentFactory {

    private IQInlineSuggestionSegmentFactory() {
    }

    private enum BracketType {
        OPEN, CLOSE, NONE;
    }

    public static List<IQInlineSuggestionSegment> getSegmentsFromSuggestion(final QInvocationSession qSes) {
        var suggestion = qSes.getCurrentSuggestion().getInsertText();
        var suggestionLines = suggestion.split("\\R");
        var res = new ArrayList<IQInlineSuggestionSegment>();
        var widget = qSes.getViewer().getTextWidget();
        int currentOffset = qSes.getInvocationOffset();
        int distanceTraversed = 0;
        Stack<QInlineSuggestionOpenBracketSegment> unresolvedBrackets = new Stack<>();
        for (int i = 0; i < suggestionLines.length; i++) {
            int startOffset;
            int endOffset;
            String currentLine = suggestionLines[i];
            StringBuilder sb;

            startOffset = currentOffset + distanceTraversed; // this line might not exist yet so we need to think of
                                                             // something more robust
            sb = new StringBuilder(currentLine);

            String currentIndent;
            if (i == 0) {
                int currentLineInDoc = widget.getLineAtOffset(currentOffset);
                String content = widget.getLine(currentLineInDoc);
                int leadingWhitespacePosition = !content.isEmpty() ? idxOfFirstNonwhiteSpace(content) : 0;
                currentIndent = content.substring(0, leadingWhitespacePosition);
            } else {
                int leadingWhitespacePosition = idxOfFirstNonwhiteSpace(currentLine);
                currentIndent = currentLine.substring(0, leadingWhitespacePosition);
            }
            for (int j = 0; j < currentLine.length(); j++) {
                char c = currentLine.charAt(j);
                switch (getBracketType(unresolvedBrackets, suggestion, distanceTraversed + j)) {
                case OPEN:
                    var openBracket = new QInlineSuggestionOpenBracketSegment(startOffset + j, currentIndent, c);
                    unresolvedBrackets.push(openBracket);
                    break;
                case CLOSE:
                    if (!unresolvedBrackets.isEmpty()) {
                        var closeBracket = new QInlineSuggestionCloseBracketSegment(startOffset + j, i,
                                currentLine.substring(0, j), c);
                        var top = unresolvedBrackets.pop();
                        if (top.isAMatch(closeBracket)) {
                            top.pairUp(closeBracket);
                            sb.setCharAt(j, ' ');
                            res.add(closeBracket);
                            res.add(top);
                        }
                    }
                    break;
                case NONE:
                default:
                    continue;
                }
            }
            distanceTraversed += sb.length() + 1; // plus one because we got rid of a \\R when we split it
            endOffset = startOffset + sb.length() - 1;
            res.add(new QInlineSuggestionNormalSegment(startOffset, endOffset, i, sb.toString()));
        }
        return res;
    }

    public static QInlineSuggestionRightContextSegment getRightCtxSegment(final String text,
            final String firstLineInSuggestion) {
        return new QInlineSuggestionRightContextSegment(text, firstLineInSuggestion);
    }

    private static BracketType getBracketType(final Stack<QInlineSuggestionOpenBracketSegment> unresolvedBrackets,
            final String input, final int idx) {
        if (isCloseBracket(input, idx, unresolvedBrackets)) {
            // TODO: enrich logic here to eliminate false positive
            return BracketType.CLOSE;
        } else if (isOpenBracket(input, idx)) {
            // TODO: enrich logic here to eliminate false positive
            return BracketType.OPEN;
        }
        return BracketType.NONE;
    }

    private static boolean isCloseBracket(final String input, final int idx,
            final Stack<QInlineSuggestionOpenBracketSegment> unresolvedBrackets) {
        char c = input.charAt(idx);
        boolean isBracket = c == ')' || c == ']' || c == '}' || c == '>' || c == '"' || c == '\'';
        if (!isBracket) {
            return false;
        }
        if (c == '"' || c == '\'') {
            return !unresolvedBrackets.isEmpty() && unresolvedBrackets.peek().getSymbol() == c;
        }
        // TODO: enrich this check to eliminate false positives
        if (idx > 0 && Character.isWhitespace(input.charAt(idx - 1)) && c == '>') {
            return false;
        }
        return true;
    }

    private static boolean isOpenBracket(final String input, final int idx) {
        char c = input.charAt(idx);
        boolean isBracket = c == '(' || c == '[' || c == '{' || c == '<' || c == '"' || c == '\'';
        if (!isBracket) {
            return false;
        }
        // TODO: enrich this check to eliminate false postives
        if (idx > 0 && Character.isWhitespace(input.charAt(idx - 1)) && c == '<') {
            return false;
        }
        return true;
    }

    private static int idxOfFirstNonwhiteSpace(final String input) {
        for (int i = 0; i < input.length(); i++) {
            if (input.charAt(i) != ' ' && input.charAt(i) != '\t') {
                return i;
            }
        }
        return input.length();
    }
}
