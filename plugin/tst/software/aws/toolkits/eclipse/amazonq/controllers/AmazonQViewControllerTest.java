// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.util.stream.Stream;

import org.eclipse.swt.SWT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.LoggingService;
import software.aws.toolkits.eclipse.amazonq.util.PluginPlatform;

public final class AmazonQViewControllerTest {
    private AmazonQViewController viewController;
    private MockedStatic<Activator> mockedActivator;
    private LoggingService mockedLogger;

    @BeforeEach
    void setUp() {
        mockedLogger = mock(LoggingService.class);
        mockedActivator = mockStatic(Activator.class);
        mockedActivator.when(Activator::getLogger).thenReturn(mockedLogger);
    }

    @AfterEach
    void tearDown() {
        mockedActivator.close();
    }

    @ParameterizedTest
    @MethodSource("provideBrowserStyleData")
    public void getBrowserStyle(final PluginPlatform platform, final int expectedStyle) {
        viewController = new AmazonQViewController(platform);
        assertEquals(expectedStyle, viewController.getBrowserStyle());
    }

    private static Stream<Arguments> provideBrowserStyleData() {
        return Stream.of(Arguments.of(PluginPlatform.WINDOWS, SWT.EDGE),
                Arguments.of(PluginPlatform.MAC, SWT.WEBKIT),
                Arguments.of(PluginPlatform.LINUX, SWT.WEBKIT));
    }

    @ParameterizedTest
    @MethodSource("provideCompatibilityData")
    void checkWebViewCompatibility(final PluginPlatform platform, final String browserType, final boolean expectedResult) {
        viewController = new AmazonQViewController(platform);

        assertFalse(viewController.hasWebViewDependency());
        viewController.checkWebViewCompatibility(browserType);

        assertEquals(expectedResult, viewController.hasWebViewDependency());
    }

    private static Stream<Arguments> provideCompatibilityData() {
        return Stream.of(Arguments.of(PluginPlatform.WINDOWS, "edge", true),
                Arguments.of(PluginPlatform.WINDOWS, "webkit", false),
                Arguments.of(PluginPlatform.WINDOWS, "ie", false),
                Arguments.of(PluginPlatform.WINDOWS, "chrome", false),
                Arguments.of(PluginPlatform.WINDOWS, "mozilla", false),
                Arguments.of(PluginPlatform.LINUX, "webkit", true), Arguments.of(PluginPlatform.LINUX, "edge", false),
                Arguments.of(PluginPlatform.LINUX, "ie", false), Arguments.of(PluginPlatform.LINUX, "chrome", false),
                Arguments.of(PluginPlatform.LINUX, "mozilla", false), Arguments.of(PluginPlatform.MAC, "webkit", true),
                Arguments.of(PluginPlatform.MAC, "edge", false), Arguments.of(PluginPlatform.MAC, "ie", false),
                Arguments.of(PluginPlatform.MAC, "chrome", false), Arguments.of(PluginPlatform.MAC, "mozilla", false));
    }
}
