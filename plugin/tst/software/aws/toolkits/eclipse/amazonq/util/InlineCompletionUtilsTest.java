// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionParams;
import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionTriggerKind;

public class InlineCompletionUtilsTest {

    private static MockedStatic<QEclipseEditorUtils> editorUtilsMock;

    @BeforeAll
    public static void setUp() {
        editorUtilsMock = mockStatic(QEclipseEditorUtils.class);
    }

    @AfterAll
    public static void tearDown() {
        if (editorUtilsMock != null) {
            editorUtilsMock.close();
        }
    }

    @Test
    void testCwParamsIncludesOpenTabFilepaths() throws Exception {
        ITextEditor editor = mock(ITextEditor.class);
        IEditorInput editorInput = mock(IEditorInput.class);
        when(editor.getEditorInput()).thenReturn(editorInput);

        ITextViewer viewer = mock(ITextViewer.class);
        IDocument document = mock(IDocument.class);
        when(viewer.getDocument()).thenReturn(document);
        when(document.getLineOfOffset(0)).thenReturn(0);
        when(document.getLineOffset(0)).thenReturn(0);

        List<String> mockPaths = Arrays.asList("/project/src/Foo.java", "/project/src/Bar.java");
        editorUtilsMock.when(() -> QEclipseEditorUtils.getOpenFileUri(any(IEditorInput.class)))
                .thenReturn(Optional.of("file:///project/src/Active.java"));
        editorUtilsMock.when(QEclipseEditorUtils::getOpenEditorFilePaths)
                .thenReturn(mockPaths);

        InlineCompletionParams params = InlineCompletionUtils.cwParamsFromContext(
                editor, viewer, 0, InlineCompletionTriggerKind.Invoke);

        assertNotNull(params.getOpenTabFilepaths());
        assertEquals(2, params.getOpenTabFilepaths().size());
        assertTrue(params.getOpenTabFilepaths().contains("/project/src/Foo.java"));
        assertTrue(params.getOpenTabFilepaths().contains("/project/src/Bar.java"));
    }

    @Test
    void testCwParamsOmitsOpenTabFilepathsWhenEmpty() throws Exception {
        ITextEditor editor = mock(ITextEditor.class);
        IEditorInput editorInput = mock(IEditorInput.class);
        when(editor.getEditorInput()).thenReturn(editorInput);

        ITextViewer viewer = mock(ITextViewer.class);
        IDocument document = mock(IDocument.class);
        when(viewer.getDocument()).thenReturn(document);
        when(document.getLineOfOffset(0)).thenReturn(0);
        when(document.getLineOffset(0)).thenReturn(0);

        editorUtilsMock.when(() -> QEclipseEditorUtils.getOpenFileUri(any(IEditorInput.class)))
                .thenReturn(Optional.of("file:///project/src/Active.java"));
        editorUtilsMock.when(QEclipseEditorUtils::getOpenEditorFilePaths)
                .thenReturn(Collections.emptyList());

        InlineCompletionParams params = InlineCompletionUtils.cwParamsFromContext(
                editor, viewer, 0, InlineCompletionTriggerKind.Invoke);

        assertNull(params.getOpenTabFilepaths());
    }

    @Test
    void testCwParamsSetsCorrectPositionAndContext() throws Exception {
        ITextEditor editor = mock(ITextEditor.class);
        IEditorInput editorInput = mock(IEditorInput.class);
        when(editor.getEditorInput()).thenReturn(editorInput);

        ITextViewer viewer = mock(ITextViewer.class);
        IDocument document = mock(IDocument.class);
        when(viewer.getDocument()).thenReturn(document);
        when(document.getLineOfOffset(25)).thenReturn(2);
        when(document.getLineOffset(2)).thenReturn(20);

        editorUtilsMock.when(() -> QEclipseEditorUtils.getOpenFileUri(any(IEditorInput.class)))
                .thenReturn(Optional.of("file:///project/src/Test.java"));
        editorUtilsMock.when(QEclipseEditorUtils::getOpenEditorFilePaths)
                .thenReturn(Arrays.asList("/project/src/Foo.java"));

        InlineCompletionParams params = InlineCompletionUtils.cwParamsFromContext(
                editor, viewer, 25, InlineCompletionTriggerKind.Automatic);

        assertEquals(2, params.getPosition().getLine());
        assertEquals(5, params.getPosition().getCharacter());
        assertEquals(InlineCompletionTriggerKind.Automatic, params.getContext().getTriggerKind());
        assertNotNull(params.getTextDocument());
        assertEquals("file:///project/src/Test.java", params.getTextDocument().getUri());
    }
}
