// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.ui.services.IDisposable;

public interface IQInlineBracket extends IDisposable {
    public void onTypeOver();

    public void onDelete();

    public void pairUp(IQInlineBracket partner);

    public boolean hasPairedUp();

    public String getAutoCloseContent(boolean isBracketSetToAutoClose, boolean isBracesSetToAutoClose,
            boolean isStringSetToAutoClose);

    public int getRelevantOffset();

    public char getSymbol();
}
