// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

public interface IQInlineTypeaheadProcessor {
    int getNewDistanceTraversedOnDeleteAndUpdateBracketState(int inputLength, int currentDistanceTraversed,
            IQInlineBracket[] brackets);

    TypeaheadProcessorInstruction preprocessDocumentChangedBuffer(int distanceTraversed, int eventOffset, String input,
            IQInlineBracket[] brackets);

    TypeaheadProcessorInstruction preprocessBufferVerifyKeyBuffer(int distanceTraversed, char input,
            IQInlineBracket[] brackets);
}
