// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.model;

import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.annotation.JsonInclude;

public final class LogInlineCompletionSessionResultsParams {
    // Session Id attached to get completion items response
    private final String sessionId;
    // Map with results of interaction with completion items/suggestions in the  UI
    private final ConcurrentHashMap<String, InlineCompletionStates> completionSessionResult;

    // Total time when items from this suggestion session were visible in UI
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long totalSessionDisplayTime;
    // Time from request invocation start to rendering of the first suggestion in the UI.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long firstCompletionDisplayLatency;
    // Length of additional characters inputed by user from when the trigger happens to when the first suggestion is about to be shown in UI
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer typeaheadLength;

    public LogInlineCompletionSessionResultsParams(final String sessionId, final ConcurrentHashMap<String, InlineCompletionStates> completionSessionResult) {
        this.sessionId = sessionId;
        this.completionSessionResult = completionSessionResult;
    }

    public Long getTotalSessionDisplayTime() {
        return totalSessionDisplayTime;
    }

    public void setTotalSessionDisplayTime(final Long totalSessionDisplayTime) {
        this.totalSessionDisplayTime = totalSessionDisplayTime;
    }

    public Long getFirstCompletionDisplayLatency() {
        return firstCompletionDisplayLatency;
    }

    public void setFirstCompletionDisplayLatency(final Long firstCompletionDisplayLatency) {
        this.firstCompletionDisplayLatency = firstCompletionDisplayLatency;
    }

    public Integer getTypeaheadLength() {
        return typeaheadLength;
    }

    public void setTypeaheadLength(final Integer typeaheadLength) {
        this.typeaheadLength = typeaheadLength;
    }

    public ConcurrentHashMap<String, InlineCompletionStates> getCompletionSessionResult() {
        return completionSessionResult;
    }

    public String getSessionId() {
        return sessionId;
    }

}
