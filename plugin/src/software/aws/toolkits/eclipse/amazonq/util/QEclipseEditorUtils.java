// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

public final class QEclipseEditorUtils {

    private QEclipseEditorUtils() {
        // Prevent instantiation
    }

    public static IWorkbenchPage getActivePage() {
        IWorkbenchWindow window = getActiveWindow();
        return window == null ? null : window.getActivePage();
    }

    public static IWorkbenchWindow getActiveWindow() {
        return PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    }

    public static IWorkbenchPart getActivePart() {
        IWorkbenchPage page = getActivePage();
        return page == null ? null : page.getActivePart();
    }

    public static ITextEditor getActiveTextEditor() {
        IWorkbenchPage activePage = getActivePage();
        return activePage == null ? null : asTextEditor(activePage.getActiveEditor());
    }

    public static ITextEditor asTextEditor(final IEditorPart editorPart) {
        if (editorPart instanceof ITextEditor) {
            return (ITextEditor) editorPart;
        } else {
            if (editorPart instanceof MultiPageEditorPart) {
                Object multiPageEditorPart;
                if (editorPart instanceof MultiPageEditorPart) {
                    multiPageEditorPart = ((MultiPageEditorPart) editorPart).getSelectedPage();
                    if (multiPageEditorPart instanceof ITextEditor) {
                        return (ITextEditor) multiPageEditorPart;
                    }
                }
            }
            return null;
        }
    }

    public static ITextViewer asTextViewer(final IEditorPart editorPart) {
        return editorPart != null ? editorPart.getAdapter(ITextViewer.class) : null;
    }

    public static ITextViewer getActiveTextViewer(final ITextEditor editor) {
        return asTextViewer(editor);
    }

    public static boolean shouldIndentVertically(final StyledText textWidget, final int zeroIndexedLine) {
        return zeroIndexedLine + 1 < textWidget.getLineCount();
    }


}
