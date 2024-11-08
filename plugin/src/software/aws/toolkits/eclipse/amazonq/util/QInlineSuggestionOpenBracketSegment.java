// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.swt.graphics.GC;

public final class QInlineSuggestionOpenBracketSegment implements IQInlineSuggestionSegment, IQInlineBracket {
    private QInlineSuggestionCloseBracketSegment closeBracket;
    private char symbol;
    private String indent;
    private int caretOffset;
    private boolean isResolved = true;

    public QInlineSuggestionOpenBracketSegment(final int caretOffset, final String indent, final char symbol) {
        this.caretOffset = caretOffset;
        this.symbol = symbol;
        this.indent = indent;
    }

    @Override
    public void pairUp(final IQInlineBracket closeBracket) {
        this.closeBracket = (QInlineSuggestionCloseBracketSegment) closeBracket;
        if (!closeBracket.hasPairedUp()) {
            closeBracket.pairUp((IQInlineBracket) this);
        }
    }

    public boolean isAMatch(final QInlineSuggestionCloseBracketSegment closeBracket) {
        switch (symbol) {
        case '<':
            return closeBracket.getSymbol() == '>';
        case '{':
            return closeBracket.getSymbol() == '}';
        case '(':
            return closeBracket.getSymbol() == ')';
        case '"':
            return closeBracket.getSymbol() == '"';
        case '\'':
            return closeBracket.getSymbol() == '\'';
        case '[':
            return closeBracket.getSymbol() == ']';
        default:
            return false;
        }
    }

    public void setResolve(final boolean isResolved) {
        this.isResolved = isResolved;
    }

    public boolean isResolved() {
        return isResolved;
    }

    public boolean hasPairedUp() {
        return closeBracket != null;
    }

    @Override
    public void render(final GC gc, final int currentCaretOffset) {
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
    public String getAutoCloseContent(final boolean isBracketSetToAutoClose,
            final boolean isAngleBracketsSetToAutoClose,
            final boolean isBracesSetToAutoClose,
            final boolean isStringSetToAutoClose) {
        if (isResolved) {
            return null;
        }

        switch (symbol) {
        case '<':
            if (!isAngleBracketsSetToAutoClose) {
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
}
