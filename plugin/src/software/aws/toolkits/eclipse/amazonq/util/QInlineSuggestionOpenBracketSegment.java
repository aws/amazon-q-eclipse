// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.swt.graphics.GC;

public class QInlineSuggestionOpenBracketSegment implements IQInlineSuggestionSegment, IQInlineBracket {
    private QInlineSuggestionCloseBracketSegment closeBracket;
    public char symbol;
    public String indent;
    public int caretOffset;
    private boolean isResolved = true;

    public QInlineSuggestionOpenBracketSegment(int caretOffset, String indent, char symbol) {
        this.caretOffset = caretOffset;
        this.symbol = symbol;
        this.indent = indent;
    }

    @Override
    public void pairUp(IQInlineBracket closeBracket) {
        this.closeBracket = (QInlineSuggestionCloseBracketSegment) closeBracket;
        if (!closeBracket.hasPairedUp()) {
            closeBracket.pairUp((IQInlineBracket) this);
        }
    }

    public boolean isAMatch(QInlineSuggestionCloseBracketSegment closeBracket) {
        switch (symbol) {
        case '<':
            return closeBracket.symbol == '>';
        case '{':
            return closeBracket.symbol == '}';
        case '(':
            return closeBracket.symbol == ')';
        case '"':
            return closeBracket.symbol == '"';
        case '\'':
            return closeBracket.symbol == '\'';
        case '[':
            return closeBracket.symbol == ']';
        default:
            return false;
        }
    }

    public void setResolve(boolean isResolved) {
        this.isResolved = isResolved;
    }

    public boolean hasPairedUp() {
        return closeBracket != null;
    }

    @Override
    public void render(GC gc, int currentCaretOffset) {
        // We never separates open brackets from the lines from which they came.
        // This is because there is never a need to highlight open brackets.
        return;
    }

    @Override
    public void onTypeOver() {
        isResolved = false;
    }

    @Override
    public void onDelete() {
        isResolved = true;
    }

    @Override
    public String getAutoCloseContent(boolean isBracketSetToAutoClose, boolean isBracesSetToAutoClose,
            boolean isStringSetToAutoClose) {
        if (isResolved) {
            return null;
        }

        switch (symbol) {
        case '<':
            if (!isBracketSetToAutoClose) {
                return null;
            }
            return ">";
        case '{':
            if (!isBracesSetToAutoClose) {
                return null;
            }
            return "\n" + indent + "}";
        case '(':
            if (!isBracketSetToAutoClose) {
                return null;
            }
            return ")";
        case '"':
            if (!isStringSetToAutoClose) {
                return null;
            }
            return "\"";
        case '\'':
            if (!isStringSetToAutoClose) {
                return null;
            }
            return "'";
        case '[':
            if (!isBracketSetToAutoClose) {
                return null;
            }
            return "]";
        default:
            return null;
        }
    }

    @Override
    public int getRelevantOffset() {
        return caretOffset;
    }

    @Override
    public char getSymbol() {
        return symbol;
    }

    @Override
    public void dispose() {
        // noop
    }
}
