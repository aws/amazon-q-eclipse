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

	public QInlineInputListener(final StyledText widget) {
		this.widget = widget;
	}

	@Override
	public void verifyKey(final VerifyEvent event) {
		var qInvocationSessionInstance = QInvocationSession.getInstance();
		if (qInvocationSessionInstance == null
				|| qInvocationSessionInstance.getState() != QInvocationSessionState.SUGGESTION_PREVIEWING) {
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
		} else {
			qInvocationSessionInstance.setCaretMovementReason(CaretMovementReason.TEXT_INPUT);
		}
		
		// Here we examine all other relevant keystrokes that may be relevant to the preview's lifetime: 
		// - CR (new line)
		// - BS (backspace)
		String currentSuggestion = qInvocationSessionInstance.getCurrentSuggestion().trim();
		switch (event.keyCode) {
		case SWT.CR:
			distanceTraversed++;
			char currentCharInSuggestion = currentSuggestion.charAt(distanceTraversed);
			if (currentCharInSuggestion != '\n' && currentCharInSuggestion != '\r') {
				qInvocationSessionInstance.transitionToDecisionMade();
				qInvocationSessionInstance.end();
				return;
			}
			qInvocationSessionInstance.setIsLastKeyNewLine(true);
			// We would also need to consider scenarios where the suggestion contains
			// formatting whitespace.
			// Should we come across them, we would need to do the following:
			// - Examine if the last key registered was a CR, if it isn't we treat it as
			// normal and examine the input verbatim
			// - Otherwise, we shall skip ahead until the first non-whitespace text in
			// the suggestion and increment `leadingWhitespaceSkipped` accordingly.
			if (distanceTraversed < currentSuggestion.length() - 1
					&& Character.isWhitespace(currentSuggestion.charAt(distanceTraversed + 1))
					&& currentSuggestion.charAt(distanceTraversed + 1) != '\n'
					&& currentSuggestion.charAt(distanceTraversed + 1) != '\r') {
				int newWs = 0;
				while (Character.isWhitespace(currentSuggestion.charAt(distanceTraversed + 1 + newWs))) {
					newWs++;
					if ((distanceTraversed + 1 + newWs) > currentSuggestion.length()) {
						break;
					}
				}
				int leadingWhitespaceSkipped = qInvocationSessionInstance.getLeadingWhitespaceSkipped();
				leadingWhitespaceSkipped += newWs;
				qInvocationSessionInstance.setLeadingWhitespaceSkipped(leadingWhitespaceSkipped);
			}
			break;
		case SWT.BS:
			// If we are traversing backwards, we need to undo the adjustments we had done
			// for the following items as we come across them:
			// - whitespace
			// - brackets (?)
			distanceTraversed--;
			if (distanceTraversed < -1) {
				qInvocationSessionInstance.transitionToDecisionMade();
				qInvocationSessionInstance.end();
				return;
			}
			isLastKeyBackspace = true;
			int leadingWhitespaceSkipped = qInvocationSessionInstance.getLeadingWhitespaceSkipped();
			int currentOffset = widget.getCaretOffset();
			if (currentOffset - 1 <= qInvocationSessionInstance
					.getHeadOffsetAtLine(widget.getLineAtOffset(currentOffset))) {
				qInvocationSessionInstance.setLeadingWhitespaceSkipped(leadingWhitespaceSkipped - 1);
				return;
			}
			break;
		case SWT.ESC:
			qInvocationSessionInstance.transitionToDecisionMade();
			qInvocationSessionInstance.end();
			break;
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
		if (qInvocationSessionInstance == null
				|| qInvocationSessionInstance.getState() != QInvocationSessionState.SUGGESTION_PREVIEWING) {
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
		IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode("org.eclipse.jdt.ui");
		// This needs to be defaulted to true. This key is only present in the
		// preference store if it is set to false.
		// Therefore if you can't find it, it has been set to true.
		boolean isAutoClosingEnabled = preferences.getBoolean("closeBrackets", true);

		// We are deliberately leaving out curly braces here (i.e. '{') because eclipse
		// does not auto close curly braces unless there is a new line entered.
		boolean isInputClosingBracket = (event.text == ")" || event.text == "]" || event.text == "\""
				|| event.text == ">");
		boolean isInputOpeningBracket = (event.text == "(" || event.text == "[" || event.text == "\""
				|| event.text == "<");
		Stack<String> closingBrackets = qInvocationSessionInstance.getClosingBrackets();

		if (isAutoClosingEnabled) {
			if (closingBrackets == null) {
				closingBrackets = new Stack<>();
			}
			if (event.text == "(") {
				closingBrackets.push(")");
			} else if (event.text == "[") {
				closingBrackets.push("]");
			} else if (event.text == "\"") {
				closingBrackets.push("\"");
			} else if (event.text == "<") {
				closingBrackets.push(">");
			} else if (isInputClosingBracket) {
				String topClosingBracket = closingBrackets.pop();
				if (!topClosingBracket.equals(event.text)) {
					System.out.println("Session terminated due to mismatching closing bracket");
					qInvocationSessionInstance.transitionToDecisionMade();
					qInvocationSessionInstance.end();
				}
			}
		}

		int distanceAdjustedForAutoClosingBrackets = (isAutoClosingEnabled
				&& (isInputOpeningBracket || isInputClosingBracket)) ? 1 : 0;
		// Note that distanceTraversed here is the zero-based index
		// We need to subtract from leading whitespace skipped the indent the editor had
		// placed the caret after at the new line.
		int newLineOffset = 0;
		boolean isLastKeyNewLine = qInvocationSessionInstance.isLastKeyNewLine();
		int leadingWhitespaceSkipped = qInvocationSessionInstance.getLeadingWhitespaceSkipped();
		int invocationOffset = qInvocationSessionInstance.getInvocationOffset();
		if (isLastKeyNewLine) {
			int currentLineInEditor = widget.getLineAtOffset(currentOffset);
			newLineOffset = currentOffset - widget.getOffsetAtLine(currentLineInEditor);
			leadingWhitespaceSkipped -= newLineOffset;
			qInvocationSessionInstance.setLeadingWhitespaceSkipped(leadingWhitespaceSkipped);
			qInvocationSessionInstance.setIsLastKeyNewLine(false);
		}
		int distanceTraversed = currentOffset - invocationOffset
				- distanceAdjustedForAutoClosingBrackets;
		this.distanceTraversed = distanceTraversed;

		System.out.println("=========================\nDistance traversed: " + distanceTraversed);
		System.out.println("Leading whitespace skipped: " + leadingWhitespaceSkipped);
		System.out.println("Distance adjusted for auto closing brackets: " + distanceAdjustedForAutoClosingBrackets);
		System.out.println("text typed: " + event.text);
		System.out.println("current char in suggestion: " + currentSuggestion.charAt(distanceTraversed));
		System.out.println("Current caret offset: " + currentOffset);
		System.out.println("Is auto closing brackets enabled: " + isAutoClosingEnabled);

		boolean isOutOfBounds = distanceTraversed >= currentSuggestion.length() || distanceTraversed < 0;
		if (isOutOfBounds || !isInputAMatch(currentSuggestion, distanceTraversed, event.text)) {
			qInvocationSessionInstance.transitionToDecisionMade();
			qInvocationSessionInstance.end();
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
