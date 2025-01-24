// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.configuration;

import com.google.gson.JsonSyntaxException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.internal.preferences.EclipsePreferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.LoggingService;

import org.osgi.service.prefs.BackingStoreException;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class DefaultPluginStoreTest {

    private static IEclipsePreferences testPreferences;
    private static PluginStore pluginStore;
    private MockedStatic<Activator> mockedActivator;
    private static LoggingService mockLogger;

    @BeforeEach
    final void setUp() {
        mockedActivator = mockStatic(Activator.class);
        mockLogger = mockLoggingService(mockedActivator);
        testPreferences = spy(new EclipsePreferences());
        pluginStore = new DefaultPluginStore(testPreferences);
    }
    @AfterEach
    final void tearDown() throws Exception {
        mockedActivator.close();
        testPreferences.clear();
    }

    @ParameterizedTest
    @ValueSource(strings = {"testKey", "", " ", "\n"})
    final void testPutAndGetKeySuccess(final String key) throws Exception {
        String value = "testValue";

        pluginStore.put(key, value);
        verify(testPreferences).put(key, value);
        verify(testPreferences).flush();
        assertEquals(value, pluginStore.get(key));
        verifyNoInteractions(mockLogger);
    }

    @Test
    void testPutOverridingValue() throws BackingStoreException {
        String key = "testKey";
        String value = "initialValue";
        String updatedValue = "updatedValue";
        pluginStore.put(key, value);
        pluginStore.put(key, updatedValue);
        verify(testPreferences).put(key, value);
        verify(testPreferences, times(2)).flush();
        assertEquals(updatedValue, pluginStore.get(key));
        verifyNoInteractions(mockLogger);
    }
    @Test
    void testPutFailure() throws BackingStoreException {
        String key = "testKey";
        String value = "testValue";
        doThrow(new BackingStoreException("test exception")).when(testPreferences).flush();

        pluginStore.put(key, value);
        verify(testPreferences).put(key, value);
        verify(testPreferences).flush();
        verify(mockLogger).warn(eq(String.format("Error while saving entry to a preference store - key: %s, value: %s", key, value)), any(Throwable.class));

    }
    @Test
    public void testGetKeyDoesNotExist() {
        String keyDNE = "keyDNE";
        //key does not exist
        assertNull(pluginStore.get(keyDNE));
    }

    @Test
    void testRemoveKey() {
        String key = "testKey";
        String value = "testValue";
        pluginStore.put(key, value);
        pluginStore.remove(key);
        assertNull(pluginStore.get(key));
    }
    @Test
    void testAddChangeListener() {
        IPreferenceChangeListener mockChangeListener = mock(IPreferenceChangeListener.class);
        pluginStore.addChangeListener(mockChangeListener);
        verify(testPreferences).addPreferenceChangeListener(mockChangeListener);
    }

    @ParameterizedTest
    @MethodSource("keyValueProvider")
    final void testPutObject(final String key, final TestObject testObj) throws BackingStoreException {
        pluginStore.putObject(key, testObj);
        TestObject returnedObj = pluginStore.getObject(key, TestObject.class);

        assertNotNull(returnedObj);
        verify(testPreferences).putByteArray(eq(key), any(byte[].class));
        verify(testPreferences).flush();
        assertEquals(testObj.getField(), returnedObj.getField());
        verifyNoInteractions(mockLogger);
    }

    @Test
    void testPutObjectWithException() throws BackingStoreException {
        String key = "testKey";
        TestObject testObj = new TestObject("someValue");

        //null key passed in
        assertThrows(NullPointerException.class, () -> pluginStore.putObject(null, testObj));

        //BackingStoreException from flush action
        doThrow(new BackingStoreException("test exception")).when(testPreferences).flush();
        pluginStore.putObject(key, testObj);
        verify(testPreferences).putByteArray(eq(key), any(byte[].class));
        verify(testPreferences).flush();
        verify(mockLogger).warn(eq(String.format("Error while saving entry to a preference store - key: %s, value: %s", key, testObj)), any(Throwable.class));
    }

    @Test
    void testGetObjectSuccess() {
        TestObject testObj = new TestObject("someValue");
        String key = "testKey";
        pluginStore.putObject(key, testObj);
        TestObject returnedObject = pluginStore.getObject(key, TestObject.class);
        assertNotNull(returnedObject);
        assertEquals(returnedObject.getField(), testObj.getField());
        verify(testPreferences).getByteArray(key, null);
    }
    @Test
    void testGetObjectNullBytes() {
        String key = "keyDNE";
        TestObject returnedObj = pluginStore.getObject(key, TestObject.class);
        verify(testPreferences).getByteArray(key, null);
        assertNull(returnedObj);
    }
    @Test
    void testGetObjectWithTypeMismatch() {
        String key = "testKey";
        String value = "someValue";
        pluginStore.putObject(key, value);

        assertThrows(JsonSyntaxException.class, () -> pluginStore.getObject(key, TestObject.class));
    }

    private LoggingService mockLoggingService(final MockedStatic<Activator> mockedActivator) {
        LoggingService mockLogger = mock(LoggingService.class);
        mockedActivator.when(Activator::getLogger).thenReturn(mockLogger);
        doNothing().when(mockLogger).error(anyString(), any(Exception.class));
        doNothing().when(mockLogger).warn(anyString(), any(Exception.class));
        return mockLogger;
    }
    private static class TestObject {
        private final String field;
        TestObject(final String field) {
            this.field = field;
        }
        public String getField() {
            return field;
        }
    }
    private static Stream<Arguments> keyValueProvider() {
        return Stream.of(
                Arguments.of("testKey", new TestObject("testValue")),
                Arguments.of("", new TestObject("blank key")),
                Arguments.of("null value", new TestObject(null)),
                Arguments.of(" ", new TestObject("key as a space")),
                Arguments.of("\n", new TestObject("key as special character")),
                Arguments.of("testKey".repeat(500), new TestObject("large key")),
                Arguments.of("largeValue".repeat(500), new TestObject("someValue".repeat(1000)))
        );
    }
}
