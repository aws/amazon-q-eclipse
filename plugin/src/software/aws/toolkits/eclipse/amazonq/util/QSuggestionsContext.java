// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionStates;

public class QSuggestionsContext {
    private List<QSuggestionContext> details = new ArrayList<>();
    private String sessionId;
    private HashMap<String, InlineCompletionStates> suggestionCompletionResults = new HashMap<String, InlineCompletionStates>();

    private long requestedAtEpoch;
    private int currentIndex = -1;

    public QSuggestionsContext() {
    }

    public final List<QSuggestionContext> getDetails() {
        return details;
    }

    public final int getCurrentIndex() {
        return currentIndex;
    }

    public final void setCurrentIndex(final int index) {
        this.currentIndex = index;
    }

    public final void incrementIndex() {
        currentIndex = (currentIndex + 1) % details.size();
    }

    public final void decrementIndex() {
        if (currentIndex - 1 < 0) {
            currentIndex = details.size() - 1;
        } else {
            currentIndex--;
        }
    }

    public final int getNumberOfSuggestions() {
        return details.size();
    }

    public final String getSessionId() {
        return sessionId;
    }

    public final void setSessionId(final String sessionId) {
        this.sessionId = sessionId;
    }

    public final long getRequestedAtEpoch() {
        return requestedAtEpoch;
    }

    public final void setRequestedAtEpoch(final long requestedAtEpoch) {
        this.requestedAtEpoch = requestedAtEpoch;
    }
}

