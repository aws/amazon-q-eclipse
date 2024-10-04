// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.ui.services.IDisposable;

public interface IQInlineBracket extends IDisposable {
    void onTypeOver();

    void onDelete();

    void pairUp(IQInlineBracket partner);

    boolean hasPairedUp();

    String getAutoCloseContent(boolean isBracketSetToAutoClose, boolean isBracesSetToAutoClose,
            boolean isStringSetToAutoClose);

    int getRelevantOffset();

    char getSymbol();
}
