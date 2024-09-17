// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import java.util.ArrayList;
import java.util.List;

public class QSuggestionsContext {
    private List<QSuggestionContext> details = new ArrayList<>();
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
}

