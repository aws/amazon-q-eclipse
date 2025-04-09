// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.inlineChat;

import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelListener;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.ui.texteditor.ITextEditor;

abstract class FoldingListener implements IAnnotationModelListener {

    ProjectionAnnotationModel attachFoldingListener(final ITextEditor editor) {
        ProjectionAnnotationModel model = editor.getAdapter(ProjectionAnnotationModel.class);
        if (model != null) {
            model.addAnnotationModelListener(this);
        }
        return model;
    }

    void removeFoldingListener(final ProjectionAnnotationModel model) {
        if (model != null) {
            model.removeAnnotationModelListener(this);
        }
    }

    @Override
    public abstract void modelChanged(IAnnotationModel model);
}
