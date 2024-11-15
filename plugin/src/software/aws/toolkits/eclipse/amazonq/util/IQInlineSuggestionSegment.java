// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.swt.graphics.GC;

public interface IQInlineSuggestionSegment {
    void render(GC gc, int currentCaretOffset);
    void cleanUp();
}
