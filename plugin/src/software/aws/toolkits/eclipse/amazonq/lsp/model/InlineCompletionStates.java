// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.model;

public final class InlineCompletionStates {
    // Indicates if suggestion has been seen by the user in the UI
    private boolean seen;
    // Indicates if suggestion accepted
    private boolean accepted;
    // Indicates if suggestion was filtered out on the client-side and marked as
    // discarded.
    private boolean discarded;

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(final boolean seen) {
        this.seen = seen;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(final boolean accepted) {
        this.accepted = accepted;
    }

    public boolean isDiscarded() {
        return discarded;
    }

    public void setDiscarded(final boolean discarded) {
        this.discarded = discarded;
    }

    @Override
    public String toString() {
        return String.format("{accepted=%b, seen=%b, discarded=%b}", accepted, seen, discarded);
    }
}
