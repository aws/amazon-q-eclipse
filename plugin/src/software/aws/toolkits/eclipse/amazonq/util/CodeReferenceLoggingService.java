//Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
//SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import software.aws.toolkits.eclipse.amazonq.views.model.ChatCodeReference;
import software.aws.toolkits.eclipse.amazonq.views.model.InlineSuggestionCodeReference;

public interface CodeReferenceLoggingService {
    void log(InlineSuggestionCodeReference codeReference);
    void log(ChatCodeReference codeReference);
}
