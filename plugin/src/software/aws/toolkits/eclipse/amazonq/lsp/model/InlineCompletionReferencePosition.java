// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.lsp.model;

public class InlineCompletionReferencePosition {
    private int startCharacter;
    private int endCharacter;

    public final void setStartCharacter(final int startCharacter) {
        this.startCharacter = startCharacter;
    }

    public final void setEndCharacter(final int endCharacter) {
        this.endCharacter = endCharacter;
    }

    public final int getStartCharacter() {
        return startCharacter;
    }

    public final int getEndCharacter() {
        return endCharacter;
    }
}
