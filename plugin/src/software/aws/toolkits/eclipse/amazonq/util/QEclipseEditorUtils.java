// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import static software.aws.toolkits.eclipse.amazonq.util.QConstants.Q_INLINE_HINT_TEXT_STYLE;

import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.ui.IEditorInput;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;

public final class QEclipseEditorUtils {

    private QEclipseEditorUtils() {
        // Prevent instantiation
    }

    public static IWorkbenchPage getActivePage() {
        IWorkbenchWindow window = getActiveWindow();
        if (window == null) {
            System.out.println("active window is null as per getActivePage");
        }
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
        if (activePage == null) {
            System.out.println("active page is null as per getActiveTextEditor");
        }
        return activePage == null ? null : asTextEditor(activePage.getActiveEditor());
    }

    public static ISelection getSelection(final ITextEditor textEditor) {
        return textEditor.getSelectionProvider().getSelection();
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

    public static Optional<String> getOpenFileUri() {
        try {
            return getOpenFilePath()
            .map(filePath -> Paths.get(filePath).toUri().toString());
        } catch (Exception e) {
            Activator.getLogger().error("Unexpected error when determining open file path", e);
            return Optional.empty();
        }
    }

    public static Optional<String> getOpenFileUri(final IEditorInput editorInput) {
        try {
            var filePath = getOpenFilePath(editorInput);
            var fileUri = Paths.get(filePath).toUri().toString();
            return Optional.of(fileUri);
        } catch (Exception e) {
            Activator.getLogger().error("Unexpected error when determining open file path", e);
            return Optional.empty();
        }
    }

    private static Optional<String> getOpenFilePath() {
        var editor = getActiveTextEditor();
        if (editor == null) {
            return Optional.empty();
        }
        return Optional.of(getOpenFilePath(editor.getEditorInput()));
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

    public static Optional<Range> getActiveSelectionRange() {
        var editor = getActiveTextEditor();
        if (editor == null) {
            return Optional.empty();
        }

        ISelection selection = getSelection(editor);
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
                return Optional.of(new Range(start, end));
            } catch (org.eclipse.jface.text.BadLocationException e) {
                Activator.getLogger().error("Error occurred while attempting to determine selected text position in editor", e);
            }
        }
        return Optional.empty();
    }

    /*
     * Inserts the given text at cursor position and returns cursor position range of the text
     */
    public static Optional<Range> insertAtCursor(final String text) {
        var editor = getActiveTextEditor();
        if (editor == null) {
            return Optional.empty();
        }

        var selection = editor.getSelectionProvider().getSelection();
        var document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        if (selection instanceof ITextSelection textSelection) {
            try {
                int startOffset = textSelection.getOffset();
                int startLine = document.getLineOfOffset(startOffset);
                int startColumn = startOffset - document.getLineOffset(startLine);
                // correctly indent/format text
                var indentation = getIndentation(document, startOffset);
                var indentedText = applyIndentation(text, indentation);

                // insert text
                document.replace(startOffset, 0, indentedText);
                // compute end offset after text is inserted
                int endOffset = startOffset + indentedText.length();
                // Move the cursor to the end of the inserted text
                editor.selectAndReveal(endOffset, 0);

                int endLine = document.getLineOfOffset(endOffset);
                int endColumn = endOffset - document.getLineOffset(endLine);

                // return the text cursor position
                var start = new Position(startLine, startColumn);
                var end = new Position(endLine, endColumn);
                return Optional.of(new Range(start, end));
            } catch (org.eclipse.jface.text.BadLocationException e) {
                Activator.getLogger().error("Error occurred while inserting at cursor in editor", e);
            }
        }
        return Optional.empty();
    }

    public static Optional<String> getSelectedText() {
        var editor = getActiveTextEditor();
        if (editor == null) {
            return Optional.empty();
        }
        ISelection selection = getSelection(editor);
        try {
            if (selection instanceof ITextSelection) {
                ITextSelection textSelection = (ITextSelection) selection;
                String selectedText = textSelection.getText();

                if (selectedText != null && !selectedText.isEmpty()) {
                    return Optional.of(selectedText);
                }
            }
        } catch (Exception e) {
            throw new AmazonQPluginException("Error occurred while retrieving selected text", e);
        }
        return Optional.empty();
    }

    private static String applyIndentation(final String text, final String indentation) {
        var lines = List.of(text.split("\n"));
        if (lines.isEmpty()) {
            return text;
        }
        // skip indenting first line
        StringBuilder indentedText = new StringBuilder(lines.get(0));
        for (int i = 1; i < lines.size(); i++) {
            indentedText.append("\n")
            .append(lines.get(i).isEmpty() ? "" : indentation) // Don't apply the gap to empty lines (eg: end of string may end in a newline)
            .append(lines.get(i));
        }

        return indentedText.toString();
    }

    private static String getIndentation(final IDocument document, final int offset) {
        try {
            int line = document.getLineOfOffset(offset);
            int lineOffset = document.getLineOffset(line);
            var content = document.get(lineOffset, offset - lineOffset);

            if (content.trim().isEmpty()) {
                // if current line is blank or contains only whitespace, return line as indentation
                return content;
            }
            return content.substring(0, content.indexOf(content.trim()));
        } catch (BadLocationException e) {
            // swallow error and return 0 indent level
            return "";
        }
    }

    public static Font getInlineTextFont(final StyledText widget, final int inlineTextStyle) {
        FontData[] fontData = widget.getFont().getFontData();
        for (FontData fontDatum : fontData) {
            fontDatum.setStyle(Q_INLINE_HINT_TEXT_STYLE);
        }
        return new Font(widget.getDisplay(), fontData);
    }

    public static Font getInlineCloseBracketFontBold(final StyledText widget) {
        Font font = widget.getFont();
        FontData[] fontData = font.getFontData();
        for (FontData fontDatum : fontData) {
            fontDatum.setStyle(fontDatum.getStyle() | SWT.BOLD);
        }
        return new Font(widget.getDisplay(), fontData);
    }

    public static QInlineInputListener getInlineInputListener(final StyledText widget) {
        return new QInlineInputListener(widget);
    }

    public static int getOffsetInFullyExpandedDocument(final ITextViewer viewer, final int caretOffsetFromWidget) {
        int adjustedOffset = caretOffsetFromWidget;
        if (viewer instanceof ITextViewerExtension5) {
            ITextViewerExtension5 extension = (ITextViewerExtension5) viewer;
            adjustedOffset = extension.widgetOffset2ModelOffset(caretOffsetFromWidget);
        }

        return adjustedOffset > -1 ? adjustedOffset : caretOffsetFromWidget;
    }

    public static QInlineTerminationListener getInlineTerminationListener() {
        return new QInlineTerminationListener();
    }

    public static IExecutionListener getAutoTriggerExecutionListener(final Consumer<String> callback) {
        return new IExecutionListener() {
            @Override
            public void notHandled(final String commandId, final NotHandledException exception) {
                return;
            }
            @Override
            public void postExecuteFailure(final String commandId, final org.eclipse.core.commands.ExecutionException exception) {
                return;
            }
            @Override
            public void postExecuteSuccess(final String commandId, final Object returnValue) {
                return;
            }
            @Override
            public void preExecute(final String commandId, final ExecutionEvent event) {
                callback.accept(commandId);
            }
        };
    }
}
