// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.broker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.reactivex.rxjava3.disposables.Disposable;
import software.aws.toolkits.eclipse.amazonq.broker.api.EventObserver;

public final class EventBrokerTest {

    private record TestEvent(String message, int id) {
    }

    private EventBroker eventBroker;

    @BeforeEach
    void setupBeforeEach() {
        eventBroker = new EventBroker();
    }

    @Test
    void testEventDelivery() {
        TestEvent testEvent = new TestEvent("test message 1", 1);
        EventObserver<TestEvent> mockObserver = mock(EventObserver.class);

        Disposable subscription = eventBroker.subscribe(TestEvent.class, mockObserver);
        eventBroker.post(testEvent);

        verify(mockObserver, timeout(100)).onEvent(testEvent);

        subscription.dispose();
    }

    @Test
    void testNullEventsIgnored() {
        EventObserver<String> mockObserver = mock(EventObserver.class);

        Disposable subscription = eventBroker.subscribe(String.class, mockObserver);
        eventBroker.post(null);

        verify(mockObserver, never()).onEvent(any(String.class));

        subscription.dispose();
    }

    @Test
    void verifyEventOrderingMaintained() {
        TestEvent firstEvent = new TestEvent("a message", 1);
        TestEvent secondEvent = new TestEvent("another message", 2);
        TestEvent thirdEvent = new TestEvent("a message", 3);

        EventObserver<TestEvent> mockObserver = mock(EventObserver.class);

        Disposable subscription = eventBroker.subscribe(TestEvent.class, mockObserver);
        eventBroker.post(firstEvent);
        eventBroker.post(secondEvent);
        eventBroker.post(thirdEvent);

        verify(mockObserver, timeout(100)).onEvent(firstEvent);
        verify(mockObserver, timeout(100)).onEvent(secondEvent);
        verify(mockObserver, timeout(100)).onEvent(thirdEvent);

        verifyNoMoreInteractions(mockObserver);

        subscription.dispose();
    }

    @Test
    void testDifferentEventTypesIsolation() {
        class OtherTestEvent {
            private final int value;

            OtherTestEvent(final int value) {
                this.value = value;
            }

            public int getValue() {
                return value;
            }
        }

        TestEvent testEvent = new TestEvent("test message", 1);
        TestEvent secondEvent = new TestEvent("test message", 2);
        OtherTestEvent otherEvent = new OtherTestEvent(42);

        EventObserver<TestEvent> testEventObserver = mock(EventObserver.class);
        EventObserver<OtherTestEvent> otherEventObserver = mock(EventObserver.class);

        Disposable testEventSubscription = eventBroker.subscribe(TestEvent.class, testEventObserver);
        Disposable otherEventSubscription = eventBroker.subscribe(OtherTestEvent.class, otherEventObserver);

        eventBroker.post(testEvent);
        eventBroker.post(otherEvent);
        eventBroker.post(secondEvent);

        verify(testEventObserver, timeout(100).times(2)).onEvent(any());
        verify(otherEventObserver, timeout(100).times(1)).onEvent(any());

        verifyNoMoreInteractions(testEventObserver);
        verifyNoMoreInteractions(otherEventObserver);

        testEventSubscription.dispose();
        otherEventSubscription.dispose();
    }

}
