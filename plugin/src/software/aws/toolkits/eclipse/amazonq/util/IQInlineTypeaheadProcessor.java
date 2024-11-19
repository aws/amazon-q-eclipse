// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.texteditor.ITextEditor;

public interface IQInlineTypeaheadProcessor {
    int getNewDistanceTraversedOnDelete(int inputLength, int currentDistanceTraversed, IQInlineBracket[] brackets);

    boolean preprocessDocumentChangedBufferAndMaybeModifyDocument(int distanceTraversed, int eventOffset, String input)
            throws BadLocationException;

    boolean preprocessBufferVerifyKeyzbufferAndMaybeModifyDocument(int distanceTraversed, char input,
            IQInlineBracket[] brackets) throws BadLocationException;
}
