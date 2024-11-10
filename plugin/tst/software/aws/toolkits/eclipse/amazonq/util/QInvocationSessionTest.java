// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import org.eclipse.ui.texteditor.ITextEditor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.service.prefs.Preferences;

import software.aws.toolkits.eclipse.amazonq.lsp.AmazonQLspServer;
import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionItem;
import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionParams;
import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionReference;
import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionResponse;
import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionTriggerKind;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.providers.LspProvider;

public class QInvocationSessionTest {

    private static final ITextEditor MOCK_EDITOR = mock(ITextEditor.class);
    private static final InlineCompletionParams POTENT_PARAM = mock(InlineCompletionParams.class);
    private static final InlineCompletionParams IMPOTENT_PARAM = mock(InlineCompletionParams.class);

    private static InlineCompletionResponse potentResponse;
    private static InlineCompletionResponse impotentResponse;

    private static MockedStatic<Platform> prefMockStatic;
    private static MockedStatic<Activator> activatorMockStatic;
    private static MockedStatic<Display> displayMockStatic;
    private static MockedStatic<ThreadingUtils> threadingUtilsMock;
    private static MockedStatic<InlineCompletionUtils> inlineCompletionUtilsMock;
    private static MockedStatic<QEclipseEditorUtils> editorUtilsMock;
    private static MockedStatic<PlatformUI> platformUIMockStatic;

    @BeforeAll
    public static void setUp() throws Exception {
        prefMockStatic = mockStatic(Platform.class, RETURNS_DEEP_STUBS);
        Preferences prefMock = mock(Preferences.class);
        prefMockStatic.when(() -> Platform.getPreferencesService().getRootNode().node(anyString()).node(anyString())).thenReturn(prefMock);
        when(prefMock.getBoolean(anyString(), any(Boolean.class))).thenReturn(true);
        when(prefMock.getInt(anyString(), any(Integer.class))).thenReturn(4);

        platformUIMockStatic = mockStatic(PlatformUI.class);
        IWorkbench wbMock = mock(IWorkbench.class);
        platformUIMockStatic.when(PlatformUI::getWorkbench).thenReturn(wbMock);

        editorUtilsMock = mockQEclipseEditorUtils();

        activatorMockStatic = mockStatic(Activator.class);
        DefaultLoginService loginSerivceMock = mock(DefaultLoginService.class, RETURNS_DEEP_STUBS);
        activatorMockStatic.when(Activator::getLoginService).thenReturn(loginSerivceMock);
        when(loginSerivceMock.getLoginDetails().get().getIsLoggedIn()).thenReturn(true);
        when(loginSerivceMock.updateToken()).thenReturn(new CompletableFuture<Void>());

        displayMockStatic = mockStatic(Display.class);
        Display displayMock = mock(Display.class);
        displayMockStatic.when(Display::getDefault).thenReturn(displayMock);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                Runnable runnable = invocation.getArgument(0);
                runnable.run();
                return null;
            }
        }).when(displayMock).asyncExec(any(Runnable.class));
    }

    @AfterAll
    public static void tearDown() {
        if (prefMockStatic != null) {
            prefMockStatic.close();
        }
        if (platformUIMockStatic != null) {
            platformUIMockStatic.close();
        }
        if (activatorMockStatic != null) {
            activatorMockStatic.close();
        }
        if (displayMockStatic != null) {
            displayMockStatic.close();
        }
        if (threadingUtilsMock != null) {
            threadingUtilsMock.close();
        }
        if (inlineCompletionUtilsMock != null) {
            inlineCompletionUtilsMock.close();
        }
        if (editorUtilsMock != null) {
            editorUtilsMock.close();
        }
    }

    @AfterEach
    public final void afterEach() {
        QInvocationSession.getInstance().endImmediately();
    }

    @Test
    void testSessionStart() throws ExecutionException {
        QInvocationSession session = QInvocationSession.getInstance();
        assertNotEquals(session, null);

        boolean isFirstStart = session.start(MOCK_EDITOR);
        assertTrue(isFirstStart);
        isFirstStart = session.start(MOCK_EDITOR);
        assertTrue(!isFirstStart);
    }

    @Test
    // Normal ending has the following things to be tested
    // - Session should not be ended if there are suggestions received
    // - Session should be ended after all requests in flight have resolved
    // - Session should not be ended if there are still requests in flight
    void testSessionEnd() throws InterruptedException, ExecutionException {
        threadingUtilsMock = mockStatic(ThreadingUtils.class);
        threadingUtilsMock.when(() -> ThreadingUtils.executeAsyncTaskAndReturnFuture(any(Runnable.class)))
                .thenAnswer(new Answer<Future<?>>() {
                    @Override
                    public Future<?> answer(final InvocationOnMock invocation) throws Throwable {
                        Runnable runnable = invocation.getArgument(0);
                        Runnable wrapper = () -> {
                            mockLspProvider();
                            mockDisplayAsyncCall();
                            mockQEclipseEditorUtils();
                            runnable.run();
                        };
                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        return executor.submit(wrapper);
                    }
                });

        QInvocationSession session = QInvocationSession.getInstance();
        session.start(MOCK_EDITOR);

        // We need to mock the Display here because the latter half of the update is
        // done on the UI thread
        inlineCompletionUtilsMock = mockStatic(InlineCompletionUtils.class);

        // Test case: when there are suggestions received
        inlineCompletionUtilsMock.when(() -> InlineCompletionUtils.cwParamsFromContext(any(ITextEditor.class),
                any(ITextViewer.class), any(Integer.class), any(InlineCompletionTriggerKind.class)))
                .thenReturn(POTENT_PARAM);
        session.invoke();
        session.awaitAllUnresolvedTasks();
        assertTrue(session.isActive());
        session.endImmediately();

        // Test case: when there are not suggestions received
        inlineCompletionUtilsMock.when(() -> InlineCompletionUtils.cwParamsFromContext(any(ITextEditor.class),
                any(ITextViewer.class), any(Integer.class), any(InlineCompletionTriggerKind.class)))
                .thenReturn(IMPOTENT_PARAM);
        session.start(MOCK_EDITOR);
        session.invoke();
        session.awaitAllUnresolvedTasks();
        assertTrue(!session.isActive());
        session.endImmediately();

        // Test case: calling end when there are still requests in flight
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(1);
        threadingUtilsMock.when(() -> ThreadingUtils.executeAsyncTaskAndReturnFuture(any(Runnable.class)))
                .thenAnswer(new Answer<Future<?>>() {
                    @Override
                    public Future<?> answer(final InvocationOnMock invocation) throws Throwable {
                        Runnable runnable = () -> {
                            try {
                                queue.take();
                            } catch (InterruptedException e) {
                                // This will print stack traces from interrupted exception for when it gets terminated forcefully
                                // It does not mean the test has failed.
                                e.printStackTrace();
                            }
                        };
                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        return executor.submit(runnable);
                    }
                });
        session.start(MOCK_EDITOR);
        session.invoke();
        session.end();
        assertTrue(session.isActive());

        // Test case: force end
        session.endImmediately();
        assertTrue(!session.isActive());
    }

    static List<InlineCompletionItem> getInlineCompletionItems() {
        ArrayList<InlineCompletionItem> items = new ArrayList<>();
        String[] itemIds = new String[] {"itemidone", "itemidtwo", "itemidthree"};
        String[] insertTexts = new String[] {"insert text one", "insert text two", "insert text three"};
        for (int i = 0; i < itemIds.length; i++) {
            InlineCompletionItem item = new InlineCompletionItem();
            item.setItemId(itemIds[i]);
            item.setInsertText(insertTexts[i]);
            item.setReferences(new InlineCompletionReference[0]);
            items.add(item);
        }
        return items;
    }

    static MockedStatic<Activator> mockLspProvider() {
        MockedStatic<Activator> localizedActivatorMock = mockStatic(Activator.class);
        LspProvider mockLspProvider = mock(LspProvider.class);
        AmazonQLspServer mockAmazonQServer = mock(AmazonQLspServer.class);
        localizedActivatorMock.when(Activator::getLspProvider).thenReturn(mockLspProvider);
        potentResponse = mock(InlineCompletionResponse.class);
        impotentResponse = mock(InlineCompletionResponse.class);
        when(potentResponse.getItems()).thenReturn(new ArrayList<>(getInlineCompletionItems()));
        when(impotentResponse.getItems()).thenReturn(Collections.emptyList());

        when(mockLspProvider.getAmazonQServer()).thenReturn(CompletableFuture.completedFuture(mockAmazonQServer));
        when(mockAmazonQServer.inlineCompletionWithReferences(POTENT_PARAM))
                .thenReturn(CompletableFuture.supplyAsync(() -> potentResponse));
        when(mockAmazonQServer.inlineCompletionWithReferences(IMPOTENT_PARAM))
                .thenReturn(CompletableFuture.supplyAsync(() -> impotentResponse));
        return localizedActivatorMock;
    }

    static MockedStatic<Display> mockDisplayAsyncCall() {
        MockedStatic<Display> displayMockStatic = mockStatic(Display.class);
        Display displayMock = mock(Display.class);
        displayMockStatic.when(Display::getDefault).thenReturn(displayMock);
        doAnswer(new Answer<Runnable>() {
            @Override
            public Runnable answer(final InvocationOnMock invocation) throws Throwable {
                Runnable runnable = invocation.getArgument(0);
                runnable.run();
                return null;
            }
        }).when(displayMock).asyncExec(any(Runnable.class));

        return displayMockStatic;
    }

    static MockedStatic<QEclipseEditorUtils> mockQEclipseEditorUtils() {
        MockedStatic<QEclipseEditorUtils> editorUtilsMock = mockStatic(QEclipseEditorUtils.class);
        ITextViewer viewerMock = mock(ITextViewer.class);
        Font fontMock = mock(Font.class);
        editorUtilsMock.when(() -> QEclipseEditorUtils.getActiveTextViewer(any(ITextEditor.class)))
                .thenReturn(viewerMock);
        editorUtilsMock.when(() -> QEclipseEditorUtils.getInlineTextFont(any(StyledText.class), any(Integer.class)))
                .thenReturn(fontMock);
        editorUtilsMock.when(() -> QEclipseEditorUtils.getInlineCloseBracketFontBold(any(StyledText.class)))
                .thenReturn(fontMock);

        QInlineInputListener inputListenerMock = mock(QInlineInputListener.class);
        editorUtilsMock.when(() -> QEclipseEditorUtils.getInlineInputListener(any(StyledText.class)))
                .thenReturn(inputListenerMock);

        StyledText mockStyledText = mock(StyledText.class);
        when(viewerMock.getTextWidget()).thenReturn(mockStyledText);
        when(mockStyledText.getCaretOffset()).thenReturn(0);

        return editorUtilsMock;
    }
}
