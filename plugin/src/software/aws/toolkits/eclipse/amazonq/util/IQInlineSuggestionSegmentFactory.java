// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Display;

public class IQInlineSuggestionSegmentFactory {
    private enum BracketType {
        OPEN, CLOSE, NADA;
    }

    public static List<IQInlineSuggestionSegment> getSegmentsFromSuggestion(QInvocationSession qSes) {
        var suggestion = qSes.getCurrentSuggestion().getInsertText();
        var suggestionLines = suggestion.split("\\R");
        var res = new ArrayList<IQInlineSuggestionSegment>();
        var widget = qSes.getViewer().getTextWidget();
        int currentOffset = widget.getCaretOffset();
        int distanceTraversed = 0;
        Stack<QInlineSuggestionOpenBracketSegment> unresolvedBrackets = new Stack<>();
        for (int i = 0; i < suggestionLines.length; i++) {
            int startOffset, endOffset;
            String currentLine = suggestionLines[i];
            StringBuilder sb;
            if (i == 0) {
                startOffset = currentOffset;
                sb = new StringBuilder(currentLine.trim());
            } else {
                startOffset = currentOffset + distanceTraversed; // this line might not exist yet so we need to think of
                                                                 // something more robust
                sb = new StringBuilder(currentLine);
            }
            
            System.out.println("At line: " + i);
            for (int j = 0; j < currentLine.length(); j++) {
                char c = currentLine.charAt(j);
                switch (getBracketType(unresolvedBrackets, suggestion, distanceTraversed + j)) {
                case OPEN:
                    var openBracket = new QInlineSuggestionOpenBracketSegment(startOffset + j, i, j,
                            c);
                    unresolvedBrackets.push(openBracket);
                    break;
                case CLOSE:
                    if (!unresolvedBrackets.isEmpty()) {
                        var closeBracket = new QInlineSuggestionCloseBracketSegment(startOffset + j,
                                i, currentLine.substring(0, j), c);
                        var top = unresolvedBrackets.pop();
                        if (top.isAMatch(closeBracket)) {
                            top.pairUp(closeBracket);
                            sb.setCharAt(j, ' ');
                            res.add(closeBracket);
                        }
                    }
                    break;
                case NADA:
                    continue;
                }
            }
            distanceTraversed += sb.length() + 1; // plus one because we got rid of a \\R when we split it
            endOffset = startOffset + sb.length() - 1;
            res.add(new QInlineSuggestionNormalSegment(startOffset, endOffset, i, sb.toString()));
        }
        return res;
    }

    private static BracketType getBracketType(Stack<QInlineSuggestionOpenBracketSegment> unresolvedBrackets,
            String input, int idx) {
        char c = input.charAt(idx);
        if (isCloseBracket(c)) {
            // TODO: enrich logic here to eliminate false positive
            return BracketType.CLOSE;
        } else if (isOpenBracket(c)) {
            // TODO: enrich logic here to eliminate false positive
            return BracketType.OPEN;
        }
        return BracketType.NADA;
    }

    private static boolean isCloseBracket(char c) {
        return c == ')' || c == ']' || c == '}' || c == '>';
    }

    private static boolean isOpenBracket(char c) {
        return c == '(' || c == '[' || c == '{' || c == '<';
    }
}
