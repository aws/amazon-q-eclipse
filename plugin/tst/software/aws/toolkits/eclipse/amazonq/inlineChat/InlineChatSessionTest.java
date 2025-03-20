//// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
//// SPDX-License-Identifier: Apache-2.0
//
//package software.aws.toolkits.eclipse.amazonq.inlineChat;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.mockStatic;
//import static org.mockito.Mockito.when;
//
//import org.eclipse.jface.text.IDocument;
//import org.eclipse.text.undo.DocumentUndoManagerRegistry;
//import org.eclipse.text.undo.IDocumentUndoManager;
//import org.eclipse.ui.IWorkbench;
//import org.eclipse.ui.IWorkbenchPage;
//import org.eclipse.ui.contexts.IContextActivation;
//import org.eclipse.ui.contexts.IContextService;
//import org.eclipse.ui.texteditor.IDocumentProvider;
//import org.eclipse.ui.texteditor.ITextEditor;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.extension.RegisterExtension;
//import org.mockito.MockedConstruction;
//import org.mockito.MockedStatic;
//
//import software.aws.toolkits.eclipse.amazonq.chat.ChatCommunicationManager;
//import software.aws.toolkits.eclipse.amazonq.extensions.implementation.ActivatorStaticMockExtension;
//import software.aws.toolkits.eclipse.amazonq.util.PluginUtils;
//import software.aws.toolkits.eclipse.amazonq.util.ThemeDetector;
//
//public class InlineChatSessionTest {
//    private static ITextEditor editor;
//    private static IDocumentProvider documentProvider;
//    private static ThemeDetector mockThemeDetector;
//    private static IContextService mockContextService;
//    private static IContextActivation mockContextActivation;
//    private static IDocumentUndoManager mockUndoManager;
//    private static IDocument document;
//    private static IWorkbenchPage mockWbPage;
//    private static IWorkbench wbMock;
//
//    private static MockedStatic<ChatCommunicationManager> chatManagerStatic;
//    private static MockedStatic<InlineChatUIManager> uiManagerStatic;
//    private static MockedStatic<InlineChatDiffManager> diffManagerStatic;
//    private static MockedStatic<PlatformWrapper> platformUiStatic;
//    private static MockedStatic<PluginUtils> pluginUtilsStatic;
//    private static MockedConstruction<ThemeDetector> themeDetectorConstruct;
//    private static MockedStatic<DocumentUndoManagerRegistry> undoRegistryStatic;
//
//    @RegisterExtension
//    private static ActivatorStaticMockExtension activatorExtension = new ActivatorStaticMockExtension();
//
//    private static ChatCommunicationManager mockChatManager;
//    private static InlineChatUIManager mockUiManager;
//    private static InlineChatDiffManager mockDiffManager;
//
//
//
//    //    @AfterEach
//    //    public void afterTest() {
//    //        InlineChatSession.getInstance().endSession();
//    //        if (platformUiStatic != null) {
//    //            platformUiStatic.close();
//    //        }
//    //        if (chatManagerStatic != null) {
//    //            chatManagerStatic.close();
//    //        }
//    //        if (uiManagerStatic != null) {
//    //            uiManagerStatic.close();
//    //        }
//    //        if (diffManagerStatic != null) {
//    //            diffManagerStatic.close();
//    //        }
//    //        if (pluginUtilsStatic != null) {
//    //            pluginUtilsStatic.close();
//    //        }
//    //        if (undoRegistryStatic != null) {
//    //            undoRegistryStatic.close();
//    //        }
//    //        if (themeDetectorConstruct != null) {
//    //            themeDetectorConstruct.close();
//    //        }
//    //    }
//
//    @BeforeEach
//    public void beforeTest() {
//        editor = mock(ITextEditor.class, RETURNS_DEEP_STUBS);
//        mockContextService = mock(IContextService.class);
//        mockContextActivation = mock(IContextActivation.class);
//        mockWbPage = mock(IWorkbenchPage.class);
//        wbMock = mock(IWorkbench.class);
//        mockUndoManager = mock(IDocumentUndoManager.class);
//        undoRegistryStatic = mockStatic(DocumentUndoManagerRegistry.class);
//        mockChatManager = mock(ChatCommunicationManager.class);
//        platformUiStatic = mockStatic(PlatformWrapper.class);
//        chatManagerStatic = mockStatic(ChatCommunicationManager.class);
//        when(mockContextService.activateContext(anyString())).thenReturn(mockContextActivation);
//        when(editor.getSite().getPage()).thenReturn(mockWbPage);
//        platformUiStatic.when(PlatformWrapper::getWorkbench).thenReturn(wbMock);
//        when(wbMock.getService(any())).thenReturn(mockContextService);
//        undoRegistryStatic.when(() -> DocumentUndoManagerRegistry.getDocumentUndoManager(document)).thenReturn(mockUndoManager);
//        chatManagerStatic.when(ChatCommunicationManager::getInstance).thenReturn(mockChatManager);
//    }
//
//    //    @Test
//    //    public void endSessionTest() {
//    //        InlineChatSession.getInstance().endSession();
//    //        verify(mockContextService).deactivateContext(mockContextActivation);
//    //        verify(mockWbPage).removePartListener(any(InlineChatSession.class));
//    //    }
//
//}
