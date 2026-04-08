// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.model;

import java.util.List;

import org.eclipse.lsp4j.TextDocumentPositionAndWorkDoneProgressParams;

public class InlineCompletionParams extends TextDocumentPositionAndWorkDoneProgressParams {

    private InlineCompletionContext context;
    private List<String> openTabFilepaths;

    public final InlineCompletionContext getContext() {
        return context;
    }

    public final void setContext(final InlineCompletionContext context) {
        this.context = context;
    }

    public final List<String> getOpenTabFilepaths() {
        return openTabFilepaths;
    }

    public final void setOpenTabFilepaths(final List<String> openTabFilepaths) {
        this.openTabFilepaths = openTabFilepaths;
    }

}
