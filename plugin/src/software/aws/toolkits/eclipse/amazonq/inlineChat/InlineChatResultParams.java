// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.inlineChat;

import software.aws.toolkits.telemetry.TelemetryDefinitions.UserDecision;

public record InlineChatResultParams(
        String requestId,
        String language,
        int inputLength,
        int numSelectedLines,
        int numSuggestionAddChars,
        int numSuggestionAddLines,
        int numSuggestionDelChars,
        int numSuggestionDelLines,
        UserDecision userDecision,
        double startLatency,
        double endLatency) {

}
