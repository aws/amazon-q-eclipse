// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;

public final class QInlineInputListener implements VerifyListener, VerifyKeyListener {
    private StyledText widget = null;
    private int distanceTraversed = 0;
    private boolean isAutoClosingEnabled = true;
    private LastKeyStrokeType lastKeyStrokeType = LastKeyStrokeType.NORMAL_INPUT;

    private enum LastKeyStrokeType {
        NORMAL_INPUT, BACKSPACE, NORMAL_BRACKET, CURLY_BRACES, OPEN_CURLY, OPEN_CURLY_FOLLOWED_BY_NEW_LINE,
    }

    public QInlineInputListener(final StyledText widget) {
        IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode("org.eclipse.jdt.ui");
        // This needs to be defaulted to true. This key is only present in the
        // preference store if it is set to false.
        // Therefore if you can't find it, it has been set to true.
        this.isAutoClosingEnabled = preferences.getBoolean("closeBrackets", true);
        this.widget = widget;
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
        String currentSuggestion = qInvocationSessionInstance.getCurrentSuggestion().getInsertText().trim();
        switch (event.keyCode) {
        case SWT.CR:
            if (lastKeyStrokeType == LastKeyStrokeType.OPEN_CURLY && isAutoClosingEnabled) {
                lastKeyStrokeType = LastKeyStrokeType.OPEN_CURLY_FOLLOWED_BY_NEW_LINE;
                // we need to unset the vertical indent prior to new line otherwise the line inserted by
                // eclipse with the closing curly braces would inherit the extra vertical indent.
                int line = widget.getLineAtOffset(widget.getCaretOffset());
                qInvocationSessionInstance.unsetVerticalIndent(line + 1);
            } else {
                lastKeyStrokeType = LastKeyStrokeType.NORMAL_INPUT;
            }
            return;
        case SWT.BS:
            if (--distanceTraversed < 0) {
                qInvocationSessionInstance.transitionToDecisionMade();
                qInvocationSessionInstance.end();
                return;
            }
            lastKeyStrokeType = LastKeyStrokeType.BACKSPACE;
            return;
        case SWT.ESC:
            qInvocationSessionInstance.transitionToDecisionMade();
            qInvocationSessionInstance.end();
            return;
        default:
        }

        // If auto closing of brackets are not enabled we can just treat them as normal
        // inputs
        // Another scenario
        if (!isAutoClosingEnabled) {
            return;
        }

        // If auto cloising of brackets are enabled, SWT will treat the open bracket
        // differently.
        // Input of the brackets will not trigger a call to verifyText.
        // Thus we have to do the typeahead verification here.
        // Note that '{' is excluded because
        switch (event.character) {
        case '<':
            if (currentSuggestion.charAt(distanceTraversed++) != '<') {
                qInvocationSessionInstance.transitionToDecisionMade();
                qInvocationSessionInstance.end();
                return;
            }
            lastKeyStrokeType = LastKeyStrokeType.NORMAL_BRACKET;
            return;
        case '>':
            if (currentSuggestion.charAt(distanceTraversed++) != '>') {
                qInvocationSessionInstance.transitionToDecisionMade();
                qInvocationSessionInstance.end();
                return;
            }
            lastKeyStrokeType = LastKeyStrokeType.NORMAL_BRACKET;
            return;
        case '(':
            if (currentSuggestion.charAt(distanceTraversed++) != '(') {
                qInvocationSessionInstance.transitionToDecisionMade();
                qInvocationSessionInstance.end();
                return;
            }
            lastKeyStrokeType = LastKeyStrokeType.NORMAL_BRACKET;
            return;
        case ')':
            if (currentSuggestion.charAt(distanceTraversed++) != ')') {
                qInvocationSessionInstance.transitionToDecisionMade();
                qInvocationSessionInstance.end();
                return;
            }
            lastKeyStrokeType = LastKeyStrokeType.NORMAL_BRACKET;
            return;
        case '[':
            if (currentSuggestion.charAt(distanceTraversed++) != '[') {
                qInvocationSessionInstance.transitionToDecisionMade();
                qInvocationSessionInstance.end();
                return;
            }
            lastKeyStrokeType = LastKeyStrokeType.NORMAL_BRACKET;
            return;
        case ']':
            if (currentSuggestion.charAt(distanceTraversed++) != ']') {
                qInvocationSessionInstance.transitionToDecisionMade();
                qInvocationSessionInstance.end();
                return;
            }
            lastKeyStrokeType = LastKeyStrokeType.NORMAL_BRACKET;
            return;
        case '{':
            if (currentSuggestion.charAt(distanceTraversed++) != '{') {
                qInvocationSessionInstance.transitionToDecisionMade();
                qInvocationSessionInstance.end();
                return;
            }
            lastKeyStrokeType = LastKeyStrokeType.OPEN_CURLY;
            return;
        case '}':
            if (currentSuggestion.charAt(distanceTraversed++) != '}') {
                qInvocationSessionInstance.transitionToDecisionMade();
                qInvocationSessionInstance.end();
                return;
            }
            lastKeyStrokeType = LastKeyStrokeType.CURLY_BRACES;
            return;
        case '"':
            if (currentSuggestion.charAt(distanceTraversed++) != '"') {
                qInvocationSessionInstance.transitionToDecisionMade();
                qInvocationSessionInstance.end();
                return;
            }
            lastKeyStrokeType = LastKeyStrokeType.NORMAL_BRACKET;
        default:
        }

        lastKeyStrokeType = LastKeyStrokeType.NORMAL_INPUT;
    }

    @Override
    public void verifyText(final VerifyEvent event) {
        String input = event.text;
        switch (lastKeyStrokeType) {
        case NORMAL_INPUT:
            break;
        case OPEN_CURLY_FOLLOWED_BY_NEW_LINE:
            input = '\n' + event.text.split("\\R")[1];
            break;
        default:
            return;
        }

        var qInvocationSessionInstance = QInvocationSession.getInstance();
        if (qInvocationSessionInstance == null || !qInvocationSessionInstance.isPreviewingSuggestions()) {
            return;
        }

        String currentSuggestion = qInvocationSessionInstance.getCurrentSuggestion().getInsertText().trim();
        int currentOffset = widget.getCaretOffset();
        qInvocationSessionInstance
                .setHasBeenTypedahead(currentOffset - qInvocationSessionInstance.getInvocationOffset() > 0);

        boolean isOutOfBounds = distanceTraversed >= currentSuggestion.length() || distanceTraversed < 0;
        if (isOutOfBounds || !isInputAMatch(currentSuggestion, distanceTraversed, input)) {
            qInvocationSessionInstance.transitionToDecisionMade();
            qInvocationSessionInstance.end();
            return;
        }
        distanceTraversed += input.length();
    }

    private boolean isInputAMatch(final String currentSuggestion, final int startIdx, final String input) {
        boolean res;
        if (input.length() > 1) {
            res = currentSuggestion.substring(startIdx, startIdx + input.length()).equals(input);
            System.out.println("This is a match: " + res);
        } else {
            res = String.valueOf(currentSuggestion.charAt(startIdx)).equals(input);
        }
        return res;
    }
}
