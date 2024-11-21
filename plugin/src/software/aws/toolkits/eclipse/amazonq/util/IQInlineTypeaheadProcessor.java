// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import java.util.regex.Pattern;

public interface IQInlineTypeaheadProcessor {
    Pattern CURLY_AUTO_CLOSE_MATCHER = Pattern.compile("\\n[ \\t]*\\n\\s*\\}");

    enum PreprocessingCategory {
        NONE, NORMAL_BRACKETS_OPEN, NORMAL_BRACKETS_CLOSE, STR_QUOTE_OPEN, STR_QUOTE_CLOSE, CURLY_BRACES
    }

    int getNewDistanceTraversedOnDeleteAndUpdateBracketState(int inputLength, int currentDistanceTraversed,
            IQInlineBracket[] brackets);

    TypeaheadProcessorInstruction preprocessDocumentChangedBuffer(int distanceTraversed, int eventOffset, String input,
            IQInlineBracket[] brackets);

    TypeaheadProcessorInstruction postProcessDocumentChangeBuffer(int distanceTraversed, int currentOffset,
            String input, IQInlineBracket[] brackets);

    TypeaheadProcessorInstruction processVerifyKeyBuffer(int distanceTraversed, char input, IQInlineBracket[] brackets);

    boolean isBracketsSetToAutoClose();

    boolean isAngleBracketsSetToAutoClose();

    boolean isBracesSetToAutoClose();

    boolean isStringSetToAutoClose();

    boolean isCurlyBracesAutoCloseDelayed();

    int getOutstandingPadding(IQInlineBracket[] brackets);
}
