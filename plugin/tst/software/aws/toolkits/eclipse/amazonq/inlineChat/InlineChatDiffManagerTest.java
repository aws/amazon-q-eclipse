// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.inlineChat;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

import software.aws.toolkits.eclipse.amazonq.chat.models.InlineChatResult;
import software.aws.toolkits.eclipse.amazonq.extensions.implementation.ActivatorStaticMockExtension;
import software.aws.toolkits.eclipse.amazonq.util.LoggingService;

public final class InlineChatDiffManagerTest {
    private InlineChatDiffManager diffManager;
    private InlineChatTask mockTask;
    private ITextEditor mockEditor;
    private IDocument mockDocument;
    private IAnnotationModel mockAnnotationModel;
    private IEditorInput mockEditorInput;
    private Display mockDisplay;
    private InlineChatResult mockChatResult;
    private MockedStatic<Display> staticDisplay;

    @RegisterExtension
    private static ActivatorStaticMockExtension activatorExtension = new ActivatorStaticMockExtension();

    @BeforeEach
    public void setUp() {
        diffManager = InlineChatDiffManager.getInstance();
        mockTask = mock(InlineChatTask.class);
        mockEditor = mock(ITextEditor.class);
        mockDocument = mock(IDocument.class);
        mockAnnotationModel = mock(IAnnotationModel.class);
        mockEditorInput = mock(IEditorInput.class);
        mockDisplay = mock(Display.class);
        mockChatResult = mock(InlineChatResult.class);
        IDocumentProvider mockDocumentProvider = mock(IDocumentProvider.class);
        staticDisplay = mockStatic(Display.class);

        when(mockTask.isActive()).thenReturn(true);
        when(mockTask.getEditor()).thenReturn(mockEditor);
        when(mockEditor.getDocumentProvider()).thenReturn(mockDocumentProvider);
        when(mockEditor.getEditorInput()).thenReturn(mockEditorInput);
        when(mockDocumentProvider.getDocument(mockEditorInput)).thenReturn(mockDocument);
        when(mockDocumentProvider.getAnnotationModel(mockEditorInput)).thenReturn(mockAnnotationModel);
        when(mockAnnotationModel.getAnnotationIterator()).thenReturn(Collections.emptyIterator());
        when(Display.getDefault()).thenReturn(mockDisplay);

        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(mockDisplay).syncExec(any(Runnable.class));

        diffManager.initNewTask(mockTask, false);
    }

    @AfterEach
    public void tearDown() {
        staticDisplay.close();
    }

    @Test
    public void testProcessDiffWithInactiveTask() throws Exception {
        when(mockTask.isActive()).thenReturn(false);
        CompletableFuture<Void> result = diffManager.processDiff(mockChatResult, true);
        assertTrue(result.isDone());
        verify(mockTask, never()).getFirstTokenTime();
    }

    @Test
    public void testProcessDiffWithPartialResultAndNoChanges() throws Exception {
        when(mockTask.isActive()).thenReturn(true);
        when(mockTask.getPreviousPartialResponse()).thenReturn("codeToDiff");
        when(mockChatResult.body()).thenReturn("codeToDiff");

        CompletableFuture<Void> result = diffManager.processDiff(mockChatResult, true);

        assertTrue(result.isDone());
        verify(mockTask, never()).getFirstTokenTime();
        verify(mockTask).getPreviousPartialResponse();
    }

    @ParameterizedTest
    @MethodSource("processDiffTestCaseProvider")
    public void testProcessDiffWithChanges(final String originalCode, final String newCode, final int numAddedLines,
        final int numDeletedLines, final boolean isPartialResult, final boolean isFirstToken)
        throws Exception {

        when(mockTask.isActive()).thenReturn(true);
        when(mockTask.hasActiveSelection()).thenReturn(true);
        when(mockTask.getOriginalCode()).thenReturn(originalCode);
        when(mockTask.getSelectionOffset()).thenReturn(0);
        when(mockTask.getPreviousDisplayLength()).thenReturn(originalCode.length());
        when(mockChatResult.body()).thenReturn(newCode);
        when(mockTask.getFirstTokenTime()).thenReturn((isFirstToken) ? -1L : 5L);

        CompletableFuture<Void> result = diffManager.processDiff(mockChatResult, isPartialResult);

        assertTrue(result.isDone());

        verify(mockDocument).replace(eq(0), anyInt(), anyString());
        verify(mockAnnotationModel, times(numAddedLines)).addAnnotation(
            argThat(annotation -> annotation.getType().contains("diffAnnotation.added")),
            any(Position.class));
        verify(mockAnnotationModel, times(numDeletedLines)).addAnnotation(
            argThat(annotation -> annotation.getType().contains("diffAnnotation.deleted")),
            any(Position.class));

        verify(mockTask).setPreviousDisplayLength(anyInt());
        verify(mockTask).setPreviousPartialResponse(eq(newCode));
        verify(mockTask).setNumDeletedLines(numDeletedLines);
        verify(mockTask).setNumAddedLines(numAddedLines);

        //verify calls that only happen on first token
        var timesFirstTokenCalled = (isFirstToken && isPartialResult) ? times(1) : never();
        verify(mockTask, timesFirstTokenCalled).setFirstTokenTime(anyLong());

        // verify calls that only happen on final result
        var timesPartialResultCalled = (isPartialResult) ? never() : times(1);
        verify(mockTask, timesPartialResultCalled).setLastTokenTime(anyLong());
        verify(mockTask, timesPartialResultCalled).setTextDiffs(any());
    }

    @Test
    public void testHandleDecisionAccept() throws Exception {

        // Setup annotations
        Annotation addedAnnotation = mock(Annotation.class);
        Annotation deletedAnnotation = mock(Annotation.class);
        Position addedPosition = new Position(0, 10);
        Position deletedPosition = new Position(20, 10);

        @SuppressWarnings("unchecked")
        Iterator<Annotation> annotationIterator = mock(Iterator.class);
        when(annotationIterator.hasNext()).thenReturn(true, true, false, true, false);
        when(annotationIterator.next()).thenReturn(addedAnnotation, deletedAnnotation, addedAnnotation);
        when(mockAnnotationModel.getAnnotationIterator()).thenReturn(annotationIterator);

        when(addedAnnotation.getType()).thenReturn("diffAnnotation.added");
        when(deletedAnnotation.getType()).thenReturn("diffAnnotation.deleted");
        when(mockAnnotationModel.getPosition(addedAnnotation)).thenReturn(addedPosition);
        when(mockAnnotationModel.getPosition(deletedAnnotation)).thenReturn(deletedPosition);

        // Setup document operations
        when(mockDocument.getLineOfOffset(anyInt())).thenReturn(2);
        when(mockDocument.getLineOffset(anyInt())).thenReturn(20);
        when(mockDocument.getLineLength(anyInt())).thenReturn(10);

        CompletableFuture<Void> result = diffManager.handleDecision(true);
        result.get();

        verify(addedAnnotation, times(2)).getType();
        verify(deletedAnnotation).getType();
        verify(annotationIterator, times(5)).hasNext();
        assertTrue(result.isDone());
        verify(mockDocument, never()).replace(0, 10, "");
        verify(mockDocument).replace(20, 10, "");
        verify(mockAnnotationModel).removeAnnotation(any(Annotation.class));
    }

    @Test
    public void testHandleDecisionDecline() throws Exception {

        // Test dark mode
        diffManager.initNewTask(mockTask, true);

        // Setup annotations
        Annotation addedAnnotation = mock(Annotation.class);
        Annotation deletedAnnotation = mock(Annotation.class);
        Position addedPosition = new Position(0, 10);
        Position deletedPosition = new Position(20, 10);

        @SuppressWarnings("unchecked")
        Iterator<Annotation> annotationIterator = mock(Iterator.class);
        when(annotationIterator.hasNext()).thenReturn(true, true, false, true, false);
        when(annotationIterator.next()).thenReturn(addedAnnotation, deletedAnnotation, deletedAnnotation);
        when(mockAnnotationModel.getAnnotationIterator()).thenReturn(annotationIterator);

        when(addedAnnotation.getType()).thenReturn("diffAnnotation.added.dark");
        when(deletedAnnotation.getType()).thenReturn("diffAnnotation.deleted.dark");
        when(mockAnnotationModel.getPosition(addedAnnotation)).thenReturn(addedPosition);
        when(mockAnnotationModel.getPosition(deletedAnnotation)).thenReturn(deletedPosition);

        // Setup document operations
        when(mockDocument.getLineOfOffset(anyInt())).thenReturn(0);
        when(mockDocument.getLineOffset(anyInt())).thenReturn(0);
        when(mockDocument.getLineLength(anyInt())).thenReturn(10);

        CompletableFuture<Void> result = diffManager.handleDecision(false);
        result.get();

        verify(addedAnnotation).getType();
        verify(deletedAnnotation, times(2)).getType();
        verify(annotationIterator, times(5)).hasNext();
        assertTrue(result.isDone());
        verify(mockDocument).replace(0, 10, "");
        verify(mockDocument, never()).replace(20, 10, "");
        verify(mockAnnotationModel).removeAnnotation(any(Annotation.class));
    }

    @Test
    public void testHandleDecisionWithException() throws Exception {
        when(mockEditor.getDocumentProvider()).thenThrow(new RuntimeException("test exception"));
        CompletableFuture<Void> result = diffManager.handleDecision(true);
        assertTrue(result.isCompletedExceptionally());
        LoggingService loggingServiceMock = activatorExtension.getMock(LoggingService.class);
        verify(loggingServiceMock).error(argThat(message -> message.contains(" inline chat results failed with: ")), any(Throwable.class));
    }

    @Test
    void testRestoreState() {
        Annotation diffAnnotation = mock(Annotation.class);
        @SuppressWarnings("unchecked")
        Iterator<Annotation> annotationIterator = mock(Iterator.class);
        when(annotationIterator.hasNext()).thenReturn(true, false);
        when(annotationIterator.next()).thenReturn(diffAnnotation);
        when(mockAnnotationModel.getAnnotationIterator()).thenReturn(annotationIterator);
        when(diffAnnotation.getType()).thenReturn("diffAnnotation.test");

        diffManager.restoreState();

        verify(mockAnnotationModel).getAnnotationIterator();
        verify(diffAnnotation).getType();
        verify(mockAnnotationModel).removeAnnotation(diffAnnotation);
    }

    @Test
    void testRestoreStateException() {
        when(mockEditor.getDocumentProvider()).thenThrow(new RuntimeException("test"));
        diffManager.restoreState();

        LoggingService loggingServiceMock = activatorExtension.getMock(LoggingService.class);
        verify(loggingServiceMock).error(
            argThat(message -> message.contains("Failed to restore state in diff manager")),
            any(Throwable.class));
    }

    private static Stream<Arguments> processDiffTestCaseProvider() {
        return Stream.of(
            // Case 1: One line modified (1 add, 1 delete), final result, not first token
            Arguments.of("line1\nline2\n", "line1\nline2 modified\n", 1, 1, false, false),
            // Case 2: One line added, no deletions, final result, first token (no-op)
            Arguments.of("hello\nworld\n", "hello\nworld\nnew line\n", 1, 0, false, true),
            // Case 3: One line deleted, no additions, final result, not first token
            Arguments.of("one\ntwo\nthree\n", "one\nthree\n", 0, 1, false, false),
            // Case 4: Multiple lines added and deleted, partial result, first token
            Arguments.of("first\nsecond\nthird", "first\nnew line\nmodified line", 2, 2, true, true),
            // Case 5: All lines changed, partial result, not first token
            Arguments.of("delete me\nand me\n", "totally\nnew\ntext\n", 3, 2, true, false),
            // Case 6: Multiple adds only, partial result, not first token
            Arguments.of("start\n", "start\nmiddle\nend\n", 2, 0, true, false),
            // Case 7: Multiple deletes only, final result, first token
            Arguments.of("one\ntwo\nthree\nfour\n", "one\nfour\n", 0, 2, false, true),
            // Case 8: Equal adds and deletes, partial result, first token
            Arguments.of("a\nb\nc\n", "x\ny\nz\n", 3, 3, true, true),
            // Case 9: Single line to multiple lines, final result, not first token
            Arguments.of("single line\n", "first line\nsecond line\nthird line\n", 3, 1, false, false));
    }

}
