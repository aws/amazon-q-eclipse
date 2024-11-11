// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

public interface IQInlineBracket {
    void onTypeOver();

    void onDelete();

    void pairUp(IQInlineBracket partner);

    boolean hasPairedUp();

    String getAutoCloseContent(boolean isBracketsSetToAutoClose, boolean isAngleBracketsSetToAutoClose,
            boolean isBracesSetToAutoClose, boolean isStringSetToAutoClose);

    int getRelevantOffset();

    char getSymbol();
}
