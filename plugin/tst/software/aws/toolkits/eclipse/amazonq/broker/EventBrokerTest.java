// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.broker;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import io.reactivex.rxjava3.disposables.Disposable;
import software.aws.toolkits.eclipse.amazonq.broker.api.EventObserver;

public final class EventBrokerTest {

    private record TestEvent(String message, int id) {
    }

    private record OtherTestEvent() {
    }

    private EventBroker eventBroker;

    @BeforeEach
    void setupBeforeEach() {
        eventBroker = new EventBroker();
    }

    @Test
    void testEventDelivery() {
        TestEvent testEvent = new TestEvent("test message 1=", 1);
        EventObserver<TestEvent> mockObserver = mock(EventObserver.class);

        Disposable subscription = eventBroker.subscribe(TestEvent.class, mockObserver);
        eventBroker.post(TestEvent.class, testEvent);

        verify(mockObserver, timeout(1000)).onEvent(testEvent);

        subscription.dispose();
    }

    @Test
    void testNullDoesNotThrowException() {
        EventObserver<TestEvent> mockObserver = mock(EventObserver.class);

        Disposable subscription = eventBroker.subscribe(TestEvent.class, mockObserver);

        assertDoesNotThrow(() -> eventBroker.post(TestEvent.class, null));

        subscription.dispose();
    }

    @Test
    void verifyEventOrderingMaintained() {
        TestEvent firstEvent = new TestEvent("a message", 1);
        TestEvent secondEvent = new TestEvent("another message", 2);
        TestEvent thirdEvent = new TestEvent("a message", 3);

        EventObserver<TestEvent> mockObserver = mock(EventObserver.class);
        InOrder inOrder = inOrder(mockObserver);

        Disposable subscription = eventBroker.subscribe(TestEvent.class, mockObserver);
        eventBroker.post(TestEvent.class, firstEvent);
        eventBroker.post(TestEvent.class, secondEvent);
        eventBroker.post(TestEvent.class, thirdEvent);

        inOrder.verify(mockObserver, timeout(100)).onEvent(firstEvent);
        inOrder.verify(mockObserver, timeout(100)).onEvent(secondEvent);
        inOrder.verify(mockObserver, timeout(100)).onEvent(thirdEvent);
        verifyNoMoreInteractions(mockObserver);

        subscription.dispose();
    }

    @Test
    void testDifferentEventTypesIsolation() {
        TestEvent testEvent = new TestEvent("test message", 1);
        TestEvent secondEvent = new TestEvent("test message", 2);
        OtherTestEvent otherEvent = new OtherTestEvent();

        EventObserver<TestEvent> testEventObserver = mock(EventObserver.class);
        EventObserver<OtherTestEvent> otherEventObserver = mock(EventObserver.class);

        Disposable testEventSubscription = eventBroker.subscribe(TestEvent.class, testEventObserver);
        Disposable otherEventSubscription = eventBroker.subscribe(OtherTestEvent.class, otherEventObserver);

        eventBroker.post(TestEvent.class, testEvent);
        eventBroker.post(OtherTestEvent.class, otherEvent);
        eventBroker.post(TestEvent.class, secondEvent);

        verify(testEventObserver, timeout(1000).times(2)).onEvent(any());
        verify(otherEventObserver, timeout(1000).times(1)).onEvent(any());

        verifyNoMoreInteractions(testEventObserver);
        verifyNoMoreInteractions(otherEventObserver);

        testEventSubscription.dispose();
        otherEventSubscription.dispose();
    }

    @Test
    void testLatestValueEmittedOnSubscription() throws InterruptedException {
        OtherTestEvent otherEvent = new OtherTestEvent();
        TestEvent firstTestEvent = new TestEvent("test message", 1);
        TestEvent secondTestEvent = new TestEvent("test message 2", 2);

        EventObserver<TestEvent> firstEventObserver = mock(EventObserver.class);
        EventObserver<TestEvent> secondEventObserver = mock(EventObserver.class);
        EventObserver<OtherTestEvent> otherEventObserver = mock(EventObserver.class);

        eventBroker.post(TestEvent.class, firstTestEvent);
        eventBroker.post(OtherTestEvent.class, otherEvent);

        Disposable firstTestEventSubscription = eventBroker.subscribe(TestEvent.class, firstEventObserver);
        Disposable otherEventSubscription = eventBroker.subscribe(OtherTestEvent.class, otherEventObserver);

        verify(firstEventObserver, timeout(100).times(1)).onEvent(firstTestEvent);

        eventBroker.post(TestEvent.class, secondTestEvent);
        eventBroker.post(OtherTestEvent.class, otherEvent);

        Thread.sleep(100);

        Disposable secondTestEventSubscription = eventBroker.subscribe(TestEvent.class, secondEventObserver);

        verify(firstEventObserver, timeout(100).times(1)).onEvent(secondTestEvent);
        verify(secondEventObserver, timeout(100).times(1)).onEvent(secondTestEvent);
        verify(otherEventObserver, timeout(100).times(2)).onEvent(otherEvent);

        firstTestEventSubscription.dispose();
        secondTestEventSubscription.dispose();
        otherEventSubscription.dispose();
    }

    @Test
    void testVerifyNoEventsEmitUnlessEventTypeMatches() {
        OtherTestEvent otherEvent = new OtherTestEvent();

        EventObserver<TestEvent> eventObserver = mock(EventObserver.class);
        EventObserver<OtherTestEvent> otherEventObserver = mock(EventObserver.class);

        eventBroker.post(OtherTestEvent.class, otherEvent);

        Disposable eventSubscription = eventBroker.subscribe(TestEvent.class, eventObserver);
        Disposable otherEventSubscription = eventBroker.subscribe(OtherTestEvent.class, otherEventObserver);

        verifyNoInteractions(eventObserver);
        verify(otherEventObserver, timeout(100).times(1)).onEvent(otherEvent);

        eventSubscription.dispose();
        otherEventSubscription.dispose();
    }

    @Test
    void testDisposeClearsAllSubscriptions() {
        EventObserver<TestEvent> eventObserver = mock(EventObserver.class);
        EventObserver<OtherTestEvent> otherEventObserver = mock(EventObserver.class);

        Disposable eventSubscription = eventBroker.subscribe(TestEvent.class, eventObserver);
        Disposable otherEventSubscription = eventBroker.subscribe(OtherTestEvent.class, otherEventObserver);

        eventBroker.dispose();

        assertTrue(eventSubscription.isDisposed());
        assertTrue(otherEventSubscription.isDisposed());
    }

    @Test
    void testSubscriptionDisposalAndReconnectionEmitsLatestEvent() {
        EventObserver<TestEvent> eventObserver = mock(EventObserver.class);
        TestEvent testEvent = new TestEvent("test message", 1);

        eventBroker.post(TestEvent.class, testEvent);

        Disposable firstEventSubscription = eventBroker.subscribe(TestEvent.class, eventObserver);
        verify(eventObserver, timeout(100).times(1)).onEvent(testEvent);
        firstEventSubscription.dispose();

        TestEvent anotherEvent = new TestEvent("test message", 2);
        eventBroker.post(TestEvent.class, anotherEvent);

        TestEvent thirdEvent = new TestEvent("test message", 3);
        eventBroker.post(TestEvent.class, thirdEvent);

        Disposable secondEventSubscription = eventBroker.subscribe(TestEvent.class, eventObserver);
        verify(eventObserver, timeout(100).times(1)).onEvent(thirdEvent);
        secondEventSubscription.dispose();
    }

}
