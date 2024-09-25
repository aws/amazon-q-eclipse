// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.lsp.model;

public class InlineCompletionReference {
    private String referenceName;
    private String referenceUrl;
    private String licenseName;
    private InlineCompletionReferencePosition position;

    public final void setReferenceName(final String referenceName) {
        this.referenceName = referenceName;
    }

    public final void setReferenceUrl(final String referenceUrl) {
        this.referenceUrl = referenceUrl;
    }

    public final void setLicenseName(final String licenseName) {
        this.licenseName = licenseName;
    }

    public final void setPosition(final InlineCompletionReferencePosition position) {
        this.position = position;
    }

    public final String getReferenceName() {
        return referenceName;
    }

    public final String getReferenceUrl() {
        return referenceUrl;
    }

    public final String getLicenseName() {
        return licenseName;
    }

    public final InlineCompletionReferencePosition getPosition() {
        return position;
    }
}
