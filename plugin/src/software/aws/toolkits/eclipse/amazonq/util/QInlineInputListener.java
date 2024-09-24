// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.util;

import java.util.Stack;

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
	private boolean isLastKeyBackspace = false;
	private boolean isAutoClosingEnabled = true;

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

		// Here we examine all other relevant keystrokes that may be relevant to the preview's lifetime: 
		// - CR (new line)
		// - BS (backspace)
		int distanceTraversed = widget.getCaretOffset() - qInvocationSessionInstance.getInvocationOffset();
		String currentSuggestion = qInvocationSessionInstance.getCurrentSuggestion().trim();
		switch (event.keyCode) {
		case SWT.CR:
			char currentCharInSuggestion = currentSuggestion.charAt(distanceTraversed);
			if (currentCharInSuggestion != '\n' && currentCharInSuggestion != '\r') {
				qInvocationSessionInstance.transitionToDecisionMade();
				qInvocationSessionInstance.end();
				return;
			}
			return;
		case SWT.BS:
		    // The distance traversed is the index at which the key was registered. 
		    // In other words, to get the distance traversed after the fact, we need to subtract one from it. 
		    System.out.println("Distance traversed (from verify key): " + distanceTraversed);
			if (distanceTraversed - 1 < 0) {
				qInvocationSessionInstance.transitionToDecisionMade();
				qInvocationSessionInstance.end();
				return;
			}
			isLastKeyBackspace = true;
			return;
		case SWT.ESC:
			qInvocationSessionInstance.transitionToDecisionMade();
			qInvocationSessionInstance.end();
			return;
		default:
		}
		
		// If auto closing of brackets are not enabled we can just treat them as normal inputs
        if (!isAutoClosingEnabled) {
            return;
        }
        
        // If auto cloising of brackets are enabled, SWT will treat the open bracket differently.
        // Input of the open brackets will not trigger a call to verifyText. 
        // Thus we have to do the typeahead verification here. 
		switch (event.character) {
        case '<':
            if (currentSuggestion.charAt(distanceTraversed) != '<') {
                qInvocationSessionInstance.transitionToDecisionMade();
                qInvocationSessionInstance.end();
                return;
            }
            return;
        case '(':
            if (currentSuggestion.charAt(distanceTraversed) != '(') {
                qInvocationSessionInstance.transitionToDecisionMade();
                qInvocationSessionInstance.end();
                return;
            }
            return;
        case '[':
            if (currentSuggestion.charAt(distanceTraversed) != '[') {
                qInvocationSessionInstance.transitionToDecisionMade();
                qInvocationSessionInstance.end();
                return;
            }
            return;
        case '{':
            if (currentSuggestion.charAt(distanceTraversed) != '{') {
                qInvocationSessionInstance.transitionToDecisionMade();
                qInvocationSessionInstance.end();
                return;
            }
            return;
        default:
        }
	}

	@Override
	public void verifyText(final VerifyEvent event) {
	    if (isLastKeyBackspace) {
	        isLastKeyBackspace = false;
	        return;
	    }

        var qInvocationSessionInstance = QInvocationSession.getInstance();
        if (qInvocationSessionInstance == null || !qInvocationSessionInstance.isPreviewingSuggestions()) {
            return;
        }

		// In effect, we would need to fulfill the following responsibilities.
		// - We identify whether text is being entered and update a state that
		// accessible by other listeners.
		// - We shall also examine said text to see if it matches the beginning of the
		// suggestion (if there is one) so we can fulfill "typeahead" functionality.
		// Here we conduct typeahead logic
		String currentSuggestion = qInvocationSessionInstance.getCurrentSuggestion().trim();
		int currentOffset = widget.getCaretOffset();
		qInvocationSessionInstance
				.setHasBeenTypedahead(currentOffset - qInvocationSessionInstance.getInvocationOffset() > 0);

		int invocationOffset = qInvocationSessionInstance.getInvocationOffset();
		int distanceTraversed = currentOffset - invocationOffset;
		this.distanceTraversed = distanceTraversed;

		System.out.println("=========================\nDistance traversed: " + distanceTraversed);
		System.out.println("text typed: " + event.text);
		System.out.println("current char in suggestion: " + currentSuggestion.charAt(distanceTraversed));
		System.out.println("Current caret offset: " + currentOffset);
		System.out.println("Is auto closing brackets enabled: " + isAutoClosingEnabled);

		boolean isOutOfBounds = distanceTraversed >= currentSuggestion.length() || distanceTraversed < 0;
		if (isOutOfBounds || !isInputAMatch(currentSuggestion, distanceTraversed, event.text)) {
			qInvocationSessionInstance.transitionToDecisionMade();
			qInvocationSessionInstance.end();
			return;
		}
	}
	
	private boolean isInputAMatch(String currentSuggestion, int startIdx, String input) {
		if (input.length() > 1) {
			return currentSuggestion.substring(startIdx, startIdx + input.length()).equals(input);
		} else {
			return String.valueOf(currentSuggestion.charAt(startIdx)).equals(input);
		}
	}
}
