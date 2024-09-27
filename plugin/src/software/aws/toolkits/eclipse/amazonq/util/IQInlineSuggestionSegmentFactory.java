// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.util;

import java.util.ArrayList;
import java.util.List;

public class IQInlineSuggestionSegmentFactory {
	public static List<IQInlineSuggestionSegment> getSegmentsFromSuggestion(QInvocationSession qSes) {
		var suggestion = qSes.getCurrentSuggestion().getInsertText();
		var suggestionLines = suggestion.split("\\R");
		var res = new ArrayList<IQInlineSuggestionSegment>();
		var widget = qSes.getViewer().getTextWidget();
		int currentOffset = widget.getCaretOffset();
		int distanceTraversed = 0;
		for (int i = 0; i < suggestionLines.length; i++) {
			int startOffset, endOffset;
			String text;
			if (i == 0) {
				startOffset = currentOffset;
				text = suggestionLines[i].trim();
			} else {
				startOffset = currentOffset + distanceTraversed; // this line might not exist yet so we need to think of
																	// something more robust
				text = suggestionLines[i];
			}
			distanceTraversed += text.length() + 1; // plus one because we got rid of a \\R when we split it
			endOffset = startOffset + text.length() - 1;
			res.add(new QInlineSuggestionNormalSegment(startOffset, endOffset, i, text));
		}

		return res;
	}
}
