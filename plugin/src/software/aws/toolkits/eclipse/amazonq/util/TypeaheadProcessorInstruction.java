// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

public final class TypeaheadProcessorInstruction {
    private boolean shouldModifyDocument = false;
    private boolean shouldModifyCaretOffset = false;
    private int caretOffset;
    private int docInsertOffset;
    private int docInsertLength;
    private String docInsertContent;

    public boolean shouldModifyDocument() {
        return shouldModifyDocument;
    }

    public void setShouldModifyDocument(final boolean shouldModifyDocument) {
        this.shouldModifyDocument = shouldModifyDocument;
    }

    public boolean shouldModifyCaretOffset() {
        return shouldModifyCaretOffset;
    }

    public void setShouldModifyCaretOffset(final boolean shouldModifyCaretPosition) {
        this.shouldModifyCaretOffset = shouldModifyCaretPosition;
    }

    public int getCaretOffset() {
        return caretOffset;
    }

    public void setCaretOffset(final int caretOffset) {
        this.caretOffset = caretOffset;
    }

    public int getDocInsertOffset() {
        return docInsertOffset;
    }

    public void setDocInsertOffset(final int docInsertOffset) {
        this.docInsertOffset = docInsertOffset;
    }

    public int getDocInsertLength() {
        return docInsertLength;
    }

    public void setDocInsertLength(final int docInsertLength) {
        this.docInsertLength = docInsertLength;
    }

    public String getDocInsertContent() {
        return docInsertContent;
    }

    public void setDocInsertContent(final String docInsertContent) {
        this.docInsertContent = docInsertContent;
    }
}
