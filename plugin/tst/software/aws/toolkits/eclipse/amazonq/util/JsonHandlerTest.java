package software.aws.toolkits.eclipse.amazonq.util;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonHandlerTest {

    private class TestObject {
        private String field;
        TestObject(final String field) {
            this.field = field;
        }
        public String getField() {
            return field;
        }
    }

    @Mock
    private static ObjectMapper mockObjectMapper;

    @Mock
    private static LoggingService mockLogger;

    private static JsonHandler jsonHandler;

    private static MockedStatic<Activator> mockedActivator;
    private static Activator mockActivator;

    @BeforeEach()
        final void setUpMocksBeforeEach() {
        mockObjectMapper = mock(ObjectMapper.class);
        jsonHandler = new JsonHandler(mockObjectMapper);
    }

    @BeforeAll
        static final void setUpMocksBeforeAll() {
        mockLogger = mock(LoggingService.class);
        mockObjectMapper = mock(ObjectMapper.class);
        mockedActivator = mockStatic(Activator.class);
        mockActivator = mock(Activator.class);
        when(mockActivator.getLogger()).thenReturn(mockLogger);
        doNothing().when(mockLogger).error(anyString(), any(Exception.class));
    }

    @AfterAll
    static final void tearDownActivator() {
        mockedActivator.close();
    }

    @Test
    public void testSerializeWhenSuccess() throws JsonProcessingException {
        TestObject objectToSerialize = new TestObject("serializedValue");
        String expectedSerialization = "{\"field\":\"serializedValue\"}";
        when(mockObjectMapper.writeValueAsString(objectToSerialize)).thenReturn(expectedSerialization);
        String result = jsonHandler.serialize(objectToSerialize);

        assertEquals(expectedSerialization, result);
        verify(mockObjectMapper).writeValueAsString(objectToSerialize);
    }

    @Test
    public void testSerializeWithExceptionThrown() throws JsonProcessingException {
        TestObject objectToSerialize = new TestObject("serializedValue");
        JsonProcessingException testExceptionThrown = new JsonProcessingException("") { };
        when(mockObjectMapper.writeValueAsString(objectToSerialize)).thenThrow(testExceptionThrown);
        String result = jsonHandler.serialize(objectToSerialize);

        assertNull(result);
        verify(mockLogger).error(eq("Error occurred while serializing object: " + objectToSerialize.toString()), eq(testExceptionThrown));
        verify(mockObjectMapper).writeValueAsString(objectToSerialize);
    }

    @Test
    public void testDeserializeSuccess() throws JsonMappingException, JsonProcessingException {
        TestObject testObject = new TestObject("deserializedValue");
        String jsonStringToDeserialize = "{\"field\":\"deserializedValue\"}";
        when(mockObjectMapper.readValue(jsonStringToDeserialize, TestObject.class)).thenReturn(testObject);
        TestObject deserializedObject = jsonHandler.deserialize(jsonStringToDeserialize, TestObject.class);

        assertEquals(testObject, deserializedObject);
        assertEquals(testObject.getField(), deserializedObject.getField());
        verify(mockObjectMapper).readValue(jsonStringToDeserialize, TestObject.class);
    }

    @Test
    public void testDeserializeWithExceptionThrown() throws JsonProcessingException {
        String jsonStringToDeserialize = "{\"field\":\"deserializedValue\"}";
        JsonProcessingException testExceptionThrown = new JsonProcessingException("") { };
        when(mockObjectMapper.readValue(jsonStringToDeserialize, TestObject.class)).thenThrow(testExceptionThrown);
        TestObject deserializedObject = jsonHandler.deserialize(jsonStringToDeserialize, TestObject.class);

        assertNull(deserializedObject);
        verify(mockObjectMapper).readValue(jsonStringToDeserialize, TestObject.class);
        verify(mockLogger).error(eq("Error occurred while deserializing jsonString: " + jsonStringToDeserialize), eq(testExceptionThrown));
    }

    @Test
    public void testConvertObjectSuccess() throws JsonProcessingException {
        TestObject testObj = new TestObject("test");
        String jsonString = "{\"value\":\"test\"}";
        TestObject expectedObj = new TestObject("test");
        when(mockObjectMapper.writeValueAsString(testObj)).thenReturn(jsonString);
        when(mockObjectMapper.readValue(jsonString, TestObject.class)).thenReturn(expectedObj);
        TestObject result = jsonHandler.convertObject(testObj, TestObject.class);

        assertEquals(expectedObj, result);
        verify(mockObjectMapper).writeValueAsString(testObj);
        verify(mockObjectMapper).readValue(jsonString, TestObject.class);
    }

    @Test
    public void testConvertObjectReturnsNull() throws JsonProcessingException {
        TestObject testObj = new TestObject("test");
        JsonProcessingException testExceptionThrown = new JsonProcessingException("") { };
        when(mockObjectMapper.writeValueAsString(testObj)).thenThrow(testExceptionThrown);
        TestObject result = jsonHandler.convertObject(testObj, TestObject.class);

        assertNull(result);
        verify(mockObjectMapper).writeValueAsString(testObj);
    }

    @Test
    public void testGetValueForKeySuccess() {
        TestObject testObject = new TestObject("test");
        ObjectNode mockNode = mock(ObjectNode.class);
        JsonNode mockJsonNode = mock(JsonNode.class);
        String key = "testKey";

        when(mockObjectMapper.valueToTree(testObject)).thenReturn(mockNode);
        when(mockNode.has(anyString())).thenReturn(true);
        when(mockNode.get(anyString())).thenReturn(mockJsonNode);
        JsonNode result = jsonHandler.getValueForKey(testObject, key);

        assertEquals(mockJsonNode, result);
        verify(mockObjectMapper).valueToTree(testObject);
        verify(mockNode).has(key);
        verify(mockNode).get(key);
    }

    @Test
    public void testGetValueForKeyFailure() {
        TestObject testObject = new TestObject("test");
        ObjectNode mockNode = mock(ObjectNode.class);
        String key = "testKey";

        when(mockObjectMapper.valueToTree(testObject)).thenReturn(mockNode);
        when(mockNode.has(anyString())).thenReturn(false);
        JsonNode result = jsonHandler.getValueForKey(testObject, key);

        assertNull(result);
        verify(mockObjectMapper).valueToTree(testObject);
        verify(mockNode).has(key);
        verify(mockNode, never()).get(key);
    }
}
