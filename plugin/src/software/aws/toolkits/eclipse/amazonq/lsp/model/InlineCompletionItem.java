// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.model;

public class InlineCompletionItem {

    private String itemId;
    private String insertText;
    private Reference[] references;

    public final String getItemId() {
        return itemId;
    }

    public final void setItemId(final String itemId) {
        this.itemId = itemId;
    }

    public final String getInsertText() {
        return insertText;
    }

    public final void setInsertText(final String insertText) {
        this.insertText = insertText;
    }

    public final Reference[] getReferences() {
        return references;
    }

    public final void setReferences(final Reference[] references) {
        this.references = references;
    }

    public class Reference {
        private String referenceName;
        private String referenceUrl;
        private String licenseName;
        private Position position;

        public final void setReferenceName(final String referenceName) {
            this.referenceName = referenceName;
        }

        public final void setReferenceUrl(final String referenceUrl) {
            this.referenceUrl = referenceUrl;
        }

        public final void setLicenseName(final String licenseName) {
            this.licenseName = licenseName;
        }

        public final void setPosition(final Position position) {
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

        public final Position getPosition() {
            return position;
        }
    }

    public class Position {
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
}
