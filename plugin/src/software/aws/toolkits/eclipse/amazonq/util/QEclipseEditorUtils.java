// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import java.nio.file.Paths;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;

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

    public static String getOpenFileUri() {
        try {
            var filePath = getOpenFilePath();
            if (filePath != null) {
                return Paths.get(filePath).toUri().toString();
            }
        } catch (Exception e) {
            PluginLogger.error("Unexpected error when determining open file path", e);
        }
        return null;
    }

    public static String getOpenFilePath() {
        var editor = getActiveTextEditor();
        if (editor == null) {
            return null;
        }
        return getOpenFilePath(editor.getEditorInput());
    }

    public static String getOpenFilePath(final IEditorInput editorInput) {
        if (editorInput instanceof FileStoreEditorInput fileStoreEditorInput) {
            return fileStoreEditorInput.getURI().getPath();
        } else if (editorInput instanceof IFileEditorInput fileEditorInput) {
            return fileEditorInput.getFile().getRawLocation().toOSString();
        } else {
            throw new AmazonQPluginException("Unexpected editor input type: " + editorInput.getClass().getName());
        }
    }

    public static Range getActiveSelectionRange() {
        var editor = getActiveTextEditor();
        if (editor == null) {
            return null;
        }

        var selection = editor.getSelectionProvider().getSelection();
        var document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        if (selection instanceof ITextSelection textSelection) {
            try {
                int startOffset = textSelection.getOffset();
                int startLine = document.getLineOfOffset(startOffset);
                int startColumn = startOffset - document.getLineOffset(startLine);

                int endOffset = startOffset + textSelection.getLength();
                int endLine = document.getLineOfOffset(endOffset);
                int endColumn = endOffset - document.getLineOffset(endLine);

                var start = new Position(startLine, startColumn);
                var end = new Position(endLine, endColumn);
                return new Range(start, end);
            } catch (org.eclipse.jface.text.BadLocationException e) {
                PluginLogger.error("Error occurred while attempting to determine selected text position in editor", e);
                return null;
            }
        }
        return null;
    }
}
