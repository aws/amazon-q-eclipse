// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.service.prefs.Preferences;

import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionItem;
import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionParams;
import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionReference;
import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionResponse;
import software.aws.toolkits.eclipse.amazonq.lsp.model.InlineCompletionTriggerKind;
import software.aws.toolkits.eclipse.amazonq.providers.LspProvider;

public class QInvocationSessionTest {

    private static final ITextEditor MOCK_EDITOR = mock(ITextEditor.class);
    private static final InlineCompletionParams POTENT_PARAM = mock(InlineCompletionParams.class);
    private static final InlineCompletionParams IMPOTENT_PARAM = mock(InlineCompletionParams.class);

    private static InlineCompletionResponse potentResponse;
    private static InlineCompletionResponse impotentResponse;

    @BeforeAll
    public static void setUp() throws Exception {
        MockedStatic<Platform> prefMockStatic = mockStatic(Platform.class, RETURNS_DEEP_STUBS);
        Preferences prefMock = mock(Preferences.class);
        prefMockStatic.when(() -> Platform.getPreferencesService().getRootNode().node(anyString()).node(anyString())).thenReturn(prefMock);
        when(prefMock.getBoolean(anyString(), any(Boolean.class))).thenReturn(true);
        when(prefMock.getInt(anyString(), any(Integer.class))).thenReturn(4);

        MockedStatic<PlatformUI> platformUIMock = mockStatic(PlatformUI.class);
        IWorkbench wbMock = mock(IWorkbench.class);
        platformUIMock.when(PlatformUI::getWorkbench).thenReturn(wbMock);

        mockQEclipseEditorUtils();

        MockedStatic<DefaultLoginService> loginServiceMockStatic = mockStatic(DefaultLoginService.class);
        DefaultLoginService loginSerivceMock = mock(DefaultLoginService.class, RETURNS_DEEP_STUBS);
        loginServiceMockStatic.when(DefaultLoginService::getInstance).thenReturn(loginSerivceMock);
        when(loginSerivceMock.getLoginDetails().get().getIsLoggedIn()).thenReturn(true);
        when(loginSerivceMock.updateToken()).thenReturn(new CompletableFuture<Void>());

        MockedStatic<Display> displayMockStatic = mockStatic(Display.class);
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
        MockedStatic<ThreadingUtils> threadingUtilsMock = mockStatic(ThreadingUtils.class);
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
        MockedStatic<InlineCompletionUtils> inlineCompletionUtilsMock = mockStatic(InlineCompletionUtils.class);

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

    static void mockLspProvider() {
        MockedStatic<LspProvider> lspProviderMock = mockStatic(LspProvider.class, RETURNS_DEEP_STUBS);
        potentResponse = mock(InlineCompletionResponse.class);
        impotentResponse = mock(InlineCompletionResponse.class);
        when(potentResponse.getItems()).thenReturn(new ArrayList<>(getInlineCompletionItems()));
        when(impotentResponse.getItems()).thenReturn(Collections.emptyList());
        lspProviderMock.when(() -> LspProvider.getAmazonQServer().get().inlineCompletionWithReferences(POTENT_PARAM))
                .thenReturn(CompletableFuture.supplyAsync(() -> potentResponse));
        lspProviderMock.when(() -> LspProvider.getAmazonQServer().get().inlineCompletionWithReferences(IMPOTENT_PARAM))
                .thenReturn(CompletableFuture.supplyAsync(() -> impotentResponse));
    }

    static void mockDisplayAsyncCall() {
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
    }

    static void mockQEclipseEditorUtils() {
        MockedStatic<QEclipseEditorUtils> editorUtilsMock = mockStatic(QEclipseEditorUtils.class);
        ITextViewer viewerMock = mock(ITextViewer.class);
        Font fontMock = mock(Font.class);
        editorUtilsMock.when(() -> QEclipseEditorUtils.getActiveTextViewer(any(ITextEditor.class)))
                .thenReturn(viewerMock);
        editorUtilsMock.when(() -> QEclipseEditorUtils.getInlineTextFont(any(StyledText.class), any(Integer.class)))
                .thenReturn(fontMock);

        QInlineInputListener inputListenerMock = mock(QInlineInputListener.class);
        editorUtilsMock.when(() -> QEclipseEditorUtils.getInlineInputListener(any(StyledText.class)))
                .thenReturn(inputListenerMock);

        StyledText mockStyledText = mock(StyledText.class);
        when(viewerMock.getTextWidget()).thenReturn(mockStyledText);
        when(mockStyledText.getCaretOffset()).thenReturn(0);
    }
}
