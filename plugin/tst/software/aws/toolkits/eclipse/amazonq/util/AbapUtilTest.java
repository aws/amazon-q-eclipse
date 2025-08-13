// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;

class AbapUtilTest {

    private MockedStatic<Platform> platformMock;

    @AfterEach
    void tearDown() {
        if (platformMock != null) {
            platformMock.close();
        }
    }

    @Test
    void isAdtEditorWithSapPackagePrefixReturnsTrue() {
        String className = "com.sap.adt.editor.SomeEditor";

        boolean result = AbapUtil.isAdtEditor(className);

        assertTrue(result);
    }

    @Test
    void isAdtEditorWithAdtPatternReturnsTrue() {
        String className = "com.sap.adt.editor";

        boolean result = AbapUtil.isAdtEditor(className);

        assertTrue(result);
    }

    @Test
    void isAdtEditorWithNullReturnsFalse() {
        String className = null;

        boolean result = AbapUtil.isAdtEditor(className);

        assertFalse(result);
    }

    @Test
    void isAdtEditorWithNonAdtClassReturnsFalse() {
        String className = "com.example.RegularEditor";

        boolean result = AbapUtil.isAdtEditor(className);

        assertFalse(result);
    }

    @Test
    void convertSemanticUriToPathWithSemanticUriReturnsConvertedPath() {
        setupPlatformMock();
        String semanticUri = "semanticfs:/test/path";

        String result = AbapUtil.convertSemanticUriToPath(semanticUri);

        assertTrue(result.contains(".metadata/.plugins/org.eclipse.core.resources.semantic/.cache/test/path"));
        assertTrue(result.startsWith("file:///"));
    }

    @Test
    void convertSemanticUriToPathWithSemanticUriReturnsConvertedPathMacOS() {
        setupPlatformMockMacOS();
        String semanticUri = "semanticfs:/test/path";

        String result = AbapUtil.convertSemanticUriToPath(semanticUri);

        assertTrue(result.contains(".metadata/.plugins/org.eclipse.core.resources.semantic/.cache/test/path"));
        assertTrue(result.startsWith("file:///"));
    }

    @Test
    void convertSemanticUriToPathWithNonSemanticUriReturnsOriginal() {
        String nonSemanticUri = "file:///regular/path";

        String result = AbapUtil.convertSemanticUriToPath(nonSemanticUri);

        assertEquals(nonSemanticUri, result);
    }

    @Test
    void convertSemanticUriToPathWithNullReturnsNull() {
        String result = AbapUtil.convertSemanticUriToPath(null);

        assertEquals(null, result);
    }

    @Test
    void convertSemanticUriToPathWithEmptyStringReturnsEmpty() {
        String result = AbapUtil.convertSemanticUriToPath("");

        assertEquals("", result);
    }

    @Test
    void getSemanticCachePathReturnsCorrectPath() {
        setupPlatformMock();
        String workspaceRelativePath = "project/file.aclass";

        String result = AbapUtil.getSemanticCachePath(workspaceRelativePath);

        assertTrue(result.contains(".metadata/.plugins/org.eclipse.core.resources.semantic/.cache/project/file.aclass"));
    }

    private void setupPlatformMock() {
        setupPlatformMockWithPaths(
            "file:///workspace/.metadata/.plugins/org.eclipse.core.resources.semantic/.cache/test/path",
            "/workspace/.metadata/.plugins/org.eclipse.core.resources.semantic",
            "/workspace/.metadata/.plugins/org.eclipse.core.resources.semantic/.cache/project/file.aclass"
        );
    }

    private void setupPlatformMockMacOS() {
        setupPlatformMockWithPaths(
            "file:///Users/user/workspace/.metadata/.plugins/org.eclipse.core.resources.semantic/.cache/test/path",
            "/Users/user/workspace/.metadata/.plugins/org.eclipse.core.resources.semantic",
            "/Users/user/workspace/.metadata/.plugins/org.eclipse.core.resources.semantic/.cache/project/file.aclass"
        );
    }

    private void setupPlatformMockWithPaths(final String uriString, final String pathString, final String finalPathString) {
        platformMock = Mockito.mockStatic(Platform.class);
        Bundle mockBundle = Mockito.mock(Bundle.class);
        IPath mockPath = Mockito.mock(IPath.class);
        IPath mockCachePath = Mockito.mock(IPath.class);
        IPath mockFinalPath = Mockito.mock(IPath.class);
        platformMock.when(() -> Platform.getBundle(AbapUtil.SEMANTIC_BUNDLE_ID)).thenReturn(mockBundle);
        platformMock.when(() -> Platform.getStateLocation(mockBundle)).thenReturn(mockPath);
        Mockito.when(mockPath.append(AbapUtil.SEMANTIC_CACHE_FOLDER)).thenReturn(mockCachePath);
        Mockito.when(mockCachePath.append(Mockito.anyString())).thenReturn(mockFinalPath);
        java.io.File mockFile = Mockito.mock(java.io.File.class);
        java.net.URI mockUri = java.net.URI.create(uriString);
        Mockito.when(mockFinalPath.toFile()).thenReturn(mockFile);
        Mockito.when(mockFile.toURI()).thenReturn(mockUri);
        Mockito.when(mockFile.exists()).thenReturn(true);
        Mockito.when(mockPath.toString()).thenReturn(pathString);
        Mockito.when(mockFinalPath.toString()).thenReturn(finalPathString);
    }
}
