// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

import software.aws.toolkits.eclipse.amazonq.extensions.implementation.ActivatorStaticMockExtension;
import software.aws.toolkits.eclipse.amazonq.lsp.manager.LspConstants;
import software.aws.toolkits.eclipse.amazonq.providers.lsp.LspManagerProvider;

class LspManagerProviderTest {

    @RegisterExtension
    private static ActivatorStaticMockExtension activatorStaticMockExtension = new ActivatorStaticMockExtension();

    private static Stream<Arguments> validManifestOverride() {
        return Stream.of(
            Arguments.of("https://custom.example.com/manifest.json", "https://custom.example.com/manifest.json"),
            Arguments.of("https://another.domain.com/path/manifest.json", "https://another.domain.com/path/manifest.json")
        );
    }

    @ParameterizedTest
    @MethodSource("validManifestOverride")
    void testGetManifestUrlWithValidOverride(final String envValue, final String expectedUrl) {
        try (MockedStatic<LspManagerProvider> mockedProvider = mockStatic(LspManagerProvider.class)) {
            mockedProvider.when(() -> LspManagerProvider.getEnvironmentVariable("Q_MANIFEST")).thenReturn(envValue);
            mockedProvider.when(() -> LspManagerProvider.getManifestUrl()).thenCallRealMethod();
            String manifestUrl = LspManagerProvider.getManifestUrl();
            assertEquals(expectedUrl, manifestUrl);
        }
    }

    private static Stream<Arguments> noManifestOverride() {
        return Stream.of(
            Arguments.of((String) null),
            Arguments.of(""),
            Arguments.of("   ")
        );
    }

    @ParameterizedTest
    @MethodSource("noManifestOverride")
    void testGetManifestUrlWithNoOverride(final String envValue) {
        try (MockedStatic<LspManagerProvider> mockedProvider = mockStatic(LspManagerProvider.class)) {
            mockedProvider.when(() -> LspManagerProvider.getEnvironmentVariable("Q_MANIFEST")).thenReturn(envValue);
            mockedProvider.when(() -> LspManagerProvider.getManifestUrl()).thenCallRealMethod();
            String manifestUrl = LspManagerProvider.getManifestUrl();
            assertEquals(LspConstants.CW_MANIFEST_URL, manifestUrl);
        }
    }

    private static Stream<Arguments> invalidManifestOverride() {
        return Stream.of(
            Arguments.of("https://custom.example.com/invalid.txt"),
            Arguments.of("not-a-url"),
            Arguments.of("https://example.com/file.xml"),
            Arguments.of("https://example.com/manifest")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidManifestOverride")
    void testGetManifestUrlWithInvalidOverride(final String envValue) {
        try (MockedStatic<LspManagerProvider> mockedProvider = mockStatic(LspManagerProvider.class)) {
            mockedProvider.when(() -> LspManagerProvider.getEnvironmentVariable("Q_MANIFEST")).thenReturn(envValue);
            mockedProvider.when(() -> LspManagerProvider.getManifestUrl()).thenCallRealMethod();
            String manifestUrl = LspManagerProvider.getManifestUrl();
            assertEquals(LspConstants.CW_MANIFEST_URL, manifestUrl);
        }
    }
}
