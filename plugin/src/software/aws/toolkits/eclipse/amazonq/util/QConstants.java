// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;

public final class QConstants {

    private QConstants() {
        // Prevent instantiation
    }

    public static final Color Q_INLINE_HINT_TEXT_COLOR = new Color(169, 183, 214);
    public static final int Q_INLINE_HINT_TEXT_STYLE = SWT.ITALIC;

    public static final List<String> Q_SCOPES = new ArrayList<>(Arrays.asList(
            "codewhisperer:conversations",
            "codewhisperer:transformations",
            "codewhisperer:taskassist",
            "codewhisperer:completions",
            "codewhisperer:analysis"
        ));
}
