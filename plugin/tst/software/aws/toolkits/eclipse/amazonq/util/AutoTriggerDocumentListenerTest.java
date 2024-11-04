// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.

package software.aws.toolkits.eclipse.amazonq.util;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutionException;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.ui.texteditor.ITextEditor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

public class AutoTriggerDocumentListenerTest {
    private static final String TEXT_STUB = "some stuff";

    private static MockedStatic<QInvocationSession> sessionMockStatic;
    private static MockedStatic<QEclipseEditorUtils> editorUtilsMockStatic;

    private static QInvocationSession sessionMock;

    @BeforeAll
    public static void setUp() {
        sessionMockStatic = mockStatic(QInvocationSession.class);
        sessionMock = mock(QInvocationSession.class, RETURNS_DEEP_STUBS);
        sessionMockStatic.when(QInvocationSession::getInstance).thenReturn(sessionMock);

        editorUtilsMockStatic = mockStatic(QEclipseEditorUtils.class);
        ITextEditor editorMock = mock(ITextEditor.class);
        editorUtilsMockStatic.when(QEclipseEditorUtils::getActiveTextEditor).thenReturn(editorMock);
    }

    @AfterAll
    public static void tearDown() {
        sessionMockStatic.close();
        editorUtilsMockStatic.close();
    }

    @Test
    void testDocumentChangeEvent() throws ExecutionException {
        // Test case: invoke should not be called under the following scenarios:
        // - Preview
        // - Decision has been made (this is relevant because the insertion of
        // suggestion would also introduce a document change
        // - Input associated with the change event has zero length
        DocumentEvent eventMock = mock(DocumentEvent.class);
        when(eventMock.getText()).thenReturn("");
        when(sessionMock.isPreviewingSuggestions()).thenReturn(true);
        when(sessionMock.isDecisionMade()).thenReturn(true);
        when(sessionMock.isActive()).thenReturn(false);
        when(sessionMock.getViewer().getTextWidget().getCaretOffset()).thenReturn(0);

        AutoTriggerDocumentListener listener = new AutoTriggerDocumentListener();

        listener.documentChanged(eventMock);
        verify(sessionMock, times(0)).invoke(any(Integer.class), any(Integer.class));
        verify(sessionMock, times(0)).start(any(ITextEditor.class));

        when(eventMock.getText()).thenReturn(TEXT_STUB);
        when(eventMock.getLength()).thenReturn(TEXT_STUB.length());
        listener.documentChanged(eventMock);
        verify(sessionMock, times(0)).invoke(any(Integer.class), any(Integer.class));

        when(sessionMock.isPreviewingSuggestions()).thenReturn(false);
        listener.documentChanged(eventMock);
        verify(sessionMock, times(0)).invoke(any(Integer.class), any(Integer.class));

        when(sessionMock.isDecisionMade()).thenReturn(false);
        listener.documentChanged(eventMock);
        verify(sessionMock, times(1)).invoke(0, TEXT_STUB.length());

        when(sessionMock.isActive()).thenReturn(true);
        listener.documentChanged(eventMock);
        verify(sessionMock, times(1)).start(any(ITextEditor.class));
        verify(sessionMock, times(2)).invoke(0, TEXT_STUB.length());
    }
}
