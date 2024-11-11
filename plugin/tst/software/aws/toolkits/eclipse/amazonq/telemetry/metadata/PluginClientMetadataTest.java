// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.telemetry.metadata;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import org.mockito.MockedStatic;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.FrameworkUtil;
import software.aws.toolkits.eclipse.amazonq.configuration.PluginStore;
import software.aws.toolkits.eclipse.amazonq.extensions.implementation.ActivatorStaticMockExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PluginClientMetadataTest {

    @RegisterExtension
    private static ActivatorStaticMockExtension activatorStaticMockExtension = new ActivatorStaticMockExtension();

    private static ClientMetadata instance;
    private static MockedStatic<Platform> mockedPlatform;
    private static MockedStatic<FrameworkUtil> mockedFrameworkUtil;

    private static String originalOsName = System.getProperty("os.name");
    private static String originalVersion = System.getProperty("os.version");
    private static String originalEclipseBuildId = System.getProperty("eclipse.buildId");

    @BeforeAll
    static void setUp() {
        System.setProperty("os.name", "testOS");
        System.setProperty("os.version", "testOSVersion");
        System.setProperty("eclipse.buildId", "testBuildId");
        mockedPlatform = mockStatic(Platform.class, RETURNS_DEEP_STUBS);
        mockedPlatform.when(() -> Platform.getProduct().getName()).thenReturn("testIdeName");
        mockedFrameworkUtil = mockStatic(FrameworkUtil.class, RETURNS_DEEP_STUBS);
        mockedFrameworkUtil.when(() ->
                FrameworkUtil.getBundle(PluginClientMetadata.class).getVersion().toString())
                .thenReturn("testPluginVersion");
        instance = PluginClientMetadata.getInstance();
    }

    @AfterAll
    static void tearDown() {
        if (mockedPlatform != null) {
            mockedPlatform.close();
        }
        if (mockedFrameworkUtil != null) {
            mockedFrameworkUtil.close();
        }
        if (originalOsName != null) {
            System.setProperty("os.name", originalOsName);
        }
        if (originalVersion != null) {
            System.setProperty("os.version", originalVersion);
        }
        if (originalEclipseBuildId != null) {
            System.setProperty("eclipse.buildId", originalEclipseBuildId);
        }
    }

    @Test
    void testGetInstance() {
        ClientMetadata secondInstance = PluginClientMetadata.getInstance();
        assertEquals(instance, secondInstance);
    }

    @Test
    void testGetOsName() {
        assertEquals("testOS", instance.getOSName());
    }

    @Test
    void testGetOsVersion() {
        assertEquals("testOSVersion", instance.getOSVersion());
    }

    @Test
    void testGetIdeName() {
        assertEquals("testIdeName", instance.getIdeName());
    }

    @Test
    void testGetIdeVersion() {
        assertEquals("testBuildId", instance.getIdeVersion());
    }

    @Test
    void testGetPluginName() {
        assertEquals("Amazon Q For Eclipse", instance.getPluginName());
    }
    @Test
    void testGetPluginVersion() {
        assertEquals("testPluginVersion", instance.getPluginVersion());
    }

    @Test
    void testGetClientIdExists() {
        String clientIdKey = "clientId";
        String expectedClientId = "testClientId";

        PluginStore pluginStoreMock = activatorStaticMockExtension.getMock(PluginStore.class);
        when(pluginStoreMock.get(clientIdKey)).thenReturn("testClientId");

        assertEquals(expectedClientId, instance.getClientId());
        verify(pluginStoreMock).get(clientIdKey);
        verify(pluginStoreMock, never()).put(eq(clientIdKey), anyString());
    }
    @Test
    void testGetClientIdWhenNull() {
        String clientIdKey = "clientId";
        String expectedClientId = "testRandomUUID";
        PluginStore pluginStoreMock = activatorStaticMockExtension.getMock(PluginStore.class);

        try (MockedStatic<UUID> mockedUUID = mockStatic(UUID.class, RETURNS_DEEP_STUBS)) {
            mockedUUID.when(() -> UUID.randomUUID().toString()).thenReturn(expectedClientId);
            when(pluginStoreMock.get(clientIdKey)).thenReturn(null);

            assertEquals(expectedClientId, instance.getClientId());
            verify(pluginStoreMock, times(2)).get(clientIdKey);
            verify(pluginStoreMock).put(clientIdKey, expectedClientId);
        }
    }
}
