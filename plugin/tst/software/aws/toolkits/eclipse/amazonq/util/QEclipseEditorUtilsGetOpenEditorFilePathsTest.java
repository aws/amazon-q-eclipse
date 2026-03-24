// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import software.aws.toolkits.eclipse.amazonq.editor.InMemoryInput;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

public class QEclipseEditorUtilsGetOpenEditorFilePathsTest {

    private static MockedStatic<PlatformUI> platformUIMock;
    private static MockedStatic<Activator> activatorMock;
    private static IWorkbenchPage mockPage;

    @BeforeAll
    public static void setUp() {
        platformUIMock = mockStatic(PlatformUI.class);
        IWorkbench workbench = mock(IWorkbench.class);
        IWorkbenchWindow window = mock(IWorkbenchWindow.class);
        mockPage = mock(IWorkbenchPage.class);

        platformUIMock.when(PlatformUI::getWorkbench).thenReturn(workbench);
        when(workbench.getActiveWorkbenchWindow()).thenReturn(window);
        when(window.getActivePage()).thenReturn(mockPage);

        activatorMock = mockStatic(Activator.class);
        LoggingService loggingService = mock(LoggingService.class);
        activatorMock.when(Activator::getLogger).thenReturn(loggingService);
    }

    @AfterAll
    public static void tearDown() {
        if (platformUIMock != null) {
            platformUIMock.close();
        }
        if (activatorMock != null) {
            activatorMock.close();
        }
    }

    @Test
    void testReturnsFilePathsFromFileStoreEditors() throws Exception {
        FileStoreEditorInput input1 = mock(FileStoreEditorInput.class);
        when(input1.getURI()).thenReturn(new URI("file:///project/src/Foo.java"));

        FileStoreEditorInput input2 = mock(FileStoreEditorInput.class);
        when(input2.getURI()).thenReturn(new URI("file:///project/src/Bar.java"));

        IEditorReference ref1 = mock(IEditorReference.class);
        when(ref1.getEditorInput()).thenReturn(input1);

        IEditorReference ref2 = mock(IEditorReference.class);
        when(ref2.getEditorInput()).thenReturn(input2);

        when(mockPage.getEditorReferences()).thenReturn(new IEditorReference[]{ref1, ref2});

        List<String> paths = QEclipseEditorUtils.getOpenEditorFilePaths();

        assertNotNull(paths);
        assertEquals(2, paths.size());
        assertTrue(paths.contains("/project/src/Foo.java"));
        assertTrue(paths.contains("/project/src/Bar.java"));
    }

    @Test
    void testReturnsFilePathsFromIFileEditorInputs() throws Exception {
        IFileEditorInput input = mock(IFileEditorInput.class);
        IFile file = mock(IFile.class);
        IPath rawLocation = mock(IPath.class);
        when(input.getFile()).thenReturn(file);
        when(file.getRawLocation()).thenReturn(rawLocation);
        when(rawLocation.toOSString()).thenReturn("/project/src/Model.java");
        when(mockPage.findEditor(input)).thenReturn(null);

        IEditorReference ref = mock(IEditorReference.class);
        when(ref.getEditorInput()).thenReturn(input);

        when(mockPage.getEditorReferences()).thenReturn(new IEditorReference[]{ref});

        List<String> paths = QEclipseEditorUtils.getOpenEditorFilePaths();

        assertNotNull(paths);
        assertEquals(1, paths.size());
        assertTrue(paths.contains("/project/src/Model.java"));
    }

    @Test
    void testSkipsInMemoryEditors() throws Exception {
        InMemoryInput inMemoryInput = mock(InMemoryInput.class);
        IEditorReference inMemoryRef = mock(IEditorReference.class);
        when(inMemoryRef.getEditorInput()).thenReturn(inMemoryInput);

        FileStoreEditorInput fileInput = mock(FileStoreEditorInput.class);
        when(fileInput.getURI()).thenReturn(new URI("file:///project/src/Real.java"));
        IEditorReference fileRef = mock(IEditorReference.class);
        when(fileRef.getEditorInput()).thenReturn(fileInput);

        when(mockPage.getEditorReferences()).thenReturn(new IEditorReference[]{inMemoryRef, fileRef});

        List<String> paths = QEclipseEditorUtils.getOpenEditorFilePaths();

        assertEquals(1, paths.size());
        assertTrue(paths.contains("/project/src/Real.java"));
    }

    @Test
    void testReturnsEmptyListWhenNoEditorsOpen() {
        when(mockPage.getEditorReferences()).thenReturn(new IEditorReference[]{});

        List<String> paths = QEclipseEditorUtils.getOpenEditorFilePaths();

        assertNotNull(paths);
        assertTrue(paths.isEmpty());
    }

    @Test
    void testSkipsEditorsWithUnresolvableInput() throws Exception {
        IEditorReference badRef = mock(IEditorReference.class);
        when(badRef.getEditorInput()).thenThrow(new RuntimeException("Cannot resolve"));

        FileStoreEditorInput goodInput = mock(FileStoreEditorInput.class);
        when(goodInput.getURI()).thenReturn(new URI("file:///project/src/Good.java"));
        IEditorReference goodRef = mock(IEditorReference.class);
        when(goodRef.getEditorInput()).thenReturn(goodInput);

        when(mockPage.getEditorReferences()).thenReturn(new IEditorReference[]{badRef, goodRef});

        List<String> paths = QEclipseEditorUtils.getOpenEditorFilePaths();

        assertEquals(1, paths.size());
        assertTrue(paths.contains("/project/src/Good.java"));
    }

    @Test
    void testReturnsEmptyListWhenActivePageIsNull() {
        IWorkbench workbench = mock(IWorkbench.class);
        IWorkbenchWindow window = mock(IWorkbenchWindow.class);
        platformUIMock.when(PlatformUI::getWorkbench).thenReturn(workbench);
        when(workbench.getActiveWorkbenchWindow()).thenReturn(window);
        when(window.getActivePage()).thenReturn(null);

        List<String> paths = QEclipseEditorUtils.getOpenEditorFilePaths();

        assertNotNull(paths);
        assertTrue(paths.isEmpty());

        // Restore for other tests
        when(window.getActivePage()).thenReturn(mockPage);
    }
}
