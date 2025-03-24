// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.swt.graphics.GC;

public interface IQInlineSuggestionSegment {
    void render(GC gc, int currentCaretOffset);
    void cleanUp();
}
