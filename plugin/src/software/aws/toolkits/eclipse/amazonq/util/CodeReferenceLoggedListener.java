// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import software.aws.toolkits.eclipse.amazonq.views.model.CodeReferenceLogItem;

public interface CodeReferenceLoggedListener {
    void onCodeReferenceLogged(CodeReferenceLogItem codeReferenceLogItem);
}
