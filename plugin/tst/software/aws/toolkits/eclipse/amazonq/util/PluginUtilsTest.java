// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.util;

import java.nio.file.Files;

import org.eclipse.core.runtime.IPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbench;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

import software.aws.toolkits.eclipse.amazonq.exception.AmazonQPluginException;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class PluginUtilsTest {

    private MockedStatic<Activator> mockedActivator;

    @TempDir
    private java.nio.file.Path tempDir;

    @Mock
    private Bundle mockBundle;

    @BeforeEach
    public final void setUp() {
        mockedActivator = mockStatic(Activator.class);
        Activator mockActivator = mock(Activator.class);
        IPath mockPath = mock(IPath.class);
        when(mockPath.toOSString()).thenReturn(tempDir.toString());
        when(mockActivator.getStateLocation()).thenReturn(mockPath);
        mockedActivator.when(Activator::getDefault).thenReturn(mockActivator);
    }

    @AfterEach
    public final void tearDown() {
        mockedActivator.close();
    }

    @Test
    public void testGetPluginDirCreatesDirectory() throws Exception {
        String dirName = "testDirectory";
        java.nio.file.Path result = PluginUtils.getPluginDir(dirName);

        assertTrue(Files.exists(result));
        assertTrue(Files.isDirectory(result));
        assertEquals(dirName, result.getFileName().toString());
        assertTrue("Ensure created directory is within temporary location", result.startsWith(tempDir));
    }
    @Test
    public void testGetPluginDirExistingDirectory() throws Exception {
        String dirName = "existingDirectory";
        java.nio.file.Path existingDir = Files.createDirectory(tempDir.resolve(dirName));
        java.nio.file.Path result = PluginUtils.getPluginDir(dirName);

        assertEquals(existingDir, result);
        assertTrue("Returned path should fall within temporary directory", result.startsWith(tempDir));
    }
    @Test
    public void testGetPluginDirWithEmptyString() {
        java.nio.file.Path result = PluginUtils.getPluginDir("");

        assertTrue(Files.exists(result));
        assertEquals(tempDir, result);
    }

    @Test
    public void testGetResources() throws IOException {
        mockBundle = mock(Bundle.class);
        String resourcePath = "path/to/testfile.json";
        URL expectedUrl = URI.create("file://someapp/path/to/testfile.json").toURL();
        URL intermediateUrl = URI.create("file://path/tp/testfile.json").toURL();

        try (MockedStatic<FrameworkUtil> mockedFrameWorkUtil = mockStatic(FrameworkUtil.class);
            MockedStatic<FileLocator> mockedFileLocator = mockStatic(FileLocator.class)) {

                mockedFrameWorkUtil.when(() -> FrameworkUtil.getBundle(PluginUtils.class)).thenReturn(mockBundle);
                mockedFileLocator.when(() -> FileLocator.find(mockBundle, new Path(resourcePath),  null))
                    .thenReturn(intermediateUrl);
                mockedFileLocator.when(() -> FileLocator.toFileURL(intermediateUrl))
                    .thenReturn(expectedUrl);

                URL returnedUrl = PluginUtils.getResource(resourcePath);
                assertEquals(expectedUrl, returnedUrl);

                mockedFrameWorkUtil.verify(() -> FrameworkUtil.getBundle(PluginUtils.class), times(1));
                mockedFileLocator.verify(() -> FileLocator.find(mockBundle, new Path(resourcePath), null));
                mockedFileLocator.verify(() -> FileLocator.toFileURL(intermediateUrl));
        }

    }

    @Test
    public void testGetPlatformSuccess() throws AmazonQPluginException {
        //Windows
        testGetPlatformHelper(true, false, false);
        //Mac
        testGetPlatformHelper(false, true, false);
        //Linux
        testGetPlatformHelper(false, false, true);
    }
    @Test
    public void testGetPlatformWithException() throws AmazonQPluginException {
        try (MockedStatic<Platform.OS> mockedPlatformOS = mockStatic(Platform.OS.class);
            MockedStatic<Platform> mockedPlatform = mockStatic(Platform.class)) {
                mockedPlatformOS.when(() -> Platform.OS.isWindows()).thenReturn(false);
                mockedPlatformOS.when(() -> Platform.OS.isLinux()).thenReturn(false);
                mockedPlatformOS.when(() -> Platform.OS.isMac()).thenReturn(false);
                mockedPlatform.when(Platform::getOS).thenReturn("UnsupportedOS");

                assertThrows(AmazonQPluginException.class, PluginUtils::getPlatform);
        }
    }

    @Test
    public void testGetArchitecture() {
        try (MockedStatic<Platform> mockedPlatform = mockStatic(Platform.class)) {
            //x86
            mockedPlatform.when(Platform::getOSArch).thenReturn(Platform.ARCH_X86_64);
            assertEquals(PluginArchitecture.X86_64, PluginUtils.getArchitecture());

            //arm64
            mockedPlatform.when(Platform::getOSArch).thenReturn(Platform.ARCH_AARCH64);
            assertEquals(PluginArchitecture.ARM_64, PluginUtils.getArchitecture());

            //unsupported architecture
            mockedPlatform.when(Platform::getOSArch).thenReturn("unsupported_arch");
            AmazonQPluginException exception = assertThrows(AmazonQPluginException.class,
                    PluginUtils::getArchitecture);
            assertEquals("Detected unsupported architecture: unsupported_arch", exception.getMessage());
        }
    }

    @Test
    public void testOpenWebpageSuccess() throws Exception {
        IWebBrowser mockExternalBrowser = mock(IWebBrowser.class);
        try (MockedStatic<PlatformUI> mockedPlatformUI = mockStatic(PlatformUI.class, RETURNS_DEEP_STUBS)) {
            mockedPlatformUI.when(() -> PlatformUI
                    .getWorkbench()
                    .getBrowserSupport()
                    .getExternalBrowser())
                    .thenReturn(mockExternalBrowser);
            String testUrl = "https://amazon.com";
            PluginUtils.openWebpage(testUrl);
            verify(mockExternalBrowser).openURL(new URL(testUrl));
        }
    }

    @Test
    public void testOpenWebpageFailure() {
        try (MockedStatic<PlatformUI> mockedPlatformUI = mockStatic(PlatformUI.class, RETURNS_DEEP_STUBS)) {
            LoggingService mockLogger = mockLoggingService(mockedActivator);
            mockedPlatformUI.when(() -> PlatformUI
                    .getWorkbench()
                    .getBrowserSupport()
                    .getExternalBrowser())
                    .thenThrow(new PartInitException("thrown exception"));

            String testUrl = "https://amazon.com";
            PluginUtils.openWebpage(testUrl);
            verify(mockLogger).warn(eq("Error while trying to open an external web page:"), any(Throwable.class));
        }
    }

    @Test
    public void testHandleExternalLinkClickConfirmed() {
        String externalLink = "https://amazon.com";
        String expectedDialogString = "Do you want to open the external website?\n\n" + externalLink;
        try (MockedStatic<PluginUtils> mockedPluginUtils = mockStatic(PluginUtils.class)) {
                LoggingService mockLogger = mockLoggingService(mockedActivator);
                mockedPluginUtils.when(() -> PluginUtils.handleExternalLinkClick(anyString())).thenCallRealMethod();
                mockedPluginUtils.when(() -> PluginUtils.showConfirmDialog(anyString(), anyString())).thenReturn(true);
                mockedPluginUtils.when(() -> PluginUtils.openWebpage(anyString())).then(invocation -> null);

                PluginUtils.handleExternalLinkClick(externalLink);
                mockedPluginUtils.verify(() -> PluginUtils.showConfirmDialog(anyString(), eq(expectedDialogString)));
                mockedPluginUtils.verify(() -> PluginUtils.openWebpage(externalLink), times(1));
                verifyNoInteractions(mockLogger);
        }
    }
    @Test
    public void testHandleExternalLinkClickDenied() {
        String externalLink = "https://amazon.com";
        String expectedDialogString = "Do you want to open the external website?\n\n" + externalLink;
        try (MockedStatic<PluginUtils> mockedPluginUtils = mockStatic(PluginUtils.class)) {
                LoggingService mockLogger = mockLoggingService(mockedActivator);
                mockedPluginUtils.when(() -> PluginUtils.handleExternalLinkClick(anyString())).thenCallRealMethod();
                mockedPluginUtils.when(() -> PluginUtils.showConfirmDialog(anyString(), anyString())).thenReturn(false);

                PluginUtils.handleExternalLinkClick(externalLink);
                mockedPluginUtils.verify(() -> PluginUtils.showConfirmDialog(anyString(), eq(expectedDialogString)));
                mockedPluginUtils.verify(() -> PluginUtils.openWebpage(externalLink), never());
                verifyNoInteractions(mockLogger);
        }
    }
    @Test
        public void testHandleExternalLinkClickWithException() {
        String externalLink = "https://amazon.com";
        try (MockedStatic<PluginUtils> mockedPluginUtils = mockStatic(PluginUtils.class)) {
            LoggingService mockLogger = mockLoggingService(mockedActivator);
            mockedPluginUtils.when(() -> PluginUtils.handleExternalLinkClick(anyString())).thenCallRealMethod();
            mockedPluginUtils.when(() -> PluginUtils.showConfirmDialog(anyString(), anyString())).thenThrow(new RuntimeException("Test Exception"));

            PluginUtils.handleExternalLinkClick(externalLink);
            mockedPluginUtils.verify(() -> PluginUtils.showConfirmDialog(anyString(), anyString()));
            mockedPluginUtils.verify(() -> PluginUtils.openWebpage(externalLink), never());
            verify(mockLogger).error(eq("Failed to open url in browser"), any(RuntimeException.class));
        }
    }

    @Test
    public void testShowView() throws Exception {
        String testId = "testViewId";
        IWorkbench mockWorkbench = mock(IWorkbench.class);
        IWorkbenchWindow mockWindow = mock(IWorkbenchWindow.class);
        IWorkbenchPage mockPage = mock(IWorkbenchPage.class);
        try (MockedStatic<PlatformUI> mockedPlatformUI = mockStatic(PlatformUI.class)) {
            LoggingService mockLogger = mockLoggingService(mockedActivator);
            mockedPlatformUI.when(PlatformUI::getWorkbench).thenReturn(mockWorkbench);

            //window is null
            when(mockWorkbench.getActiveWorkbenchWindow()).thenReturn(null);
            PluginUtils.showView(testId);
            verifyNoInteractions(mockLogger);

            //page is null
            when(mockWorkbench.getActiveWorkbenchWindow()).thenReturn(mockWindow);
            when(mockWindow.getActivePage()).thenReturn(null);
            PluginUtils.showView(testId);
            verifyNoInteractions(mockLogger);

            //success case
            when(mockWindow.getActivePage()).thenReturn(mockPage);
            PluginUtils.showView(testId);
            verify(mockPage).showView(testId);
            verify(mockLogger).info("Showing view " + testId);

            //test failure case
            doThrow(new PartInitException("Test exception")).when(mockPage).showView(anyString());
            PluginUtils.showView(testId);
            verify(mockLogger).error(eq("Error occurred while opening view " + testId), any(Throwable.class));
        }
    }

    private LoggingService mockLoggingService(final MockedStatic<Activator> mockedActivator) {
        LoggingService mockLogger = mock(LoggingService.class);
        mockedActivator.when(Activator::getLogger).thenReturn(mockLogger);
        doNothing().when(mockLogger).error(anyString(), any(Exception.class));
        doNothing().when(mockLogger).warn(anyString(), any(Exception.class));
        return mockLogger;
    }
    private void testGetPlatformHelper(final boolean windows, final boolean mac, final boolean linux) {
        PluginPlatform expectedPlatform = (windows) ? PluginPlatform.WINDOWS : (mac) ? PluginPlatform.MAC : PluginPlatform.LINUX;
        try (MockedStatic<Platform.OS> mockPlatformOS = mockStatic(Platform.OS.class)) {
            mockPlatformOS.when(() -> Platform.OS.isWindows()).thenReturn(windows);
            mockPlatformOS.when(() -> Platform.OS.isMac()).thenReturn(mac);
            mockPlatformOS.when(() -> Platform.OS.isLinux()).thenReturn(linux);

            assertEquals(mac, Platform.OS.isMac());
            assertEquals(windows, Platform.OS.isWindows());
            assertEquals(linux, Platform.OS.isLinux());
            assertEquals(expectedPlatform, PluginUtils.getPlatform());
        }
    }
}
