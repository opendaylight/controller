/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.google.common.testing.FakeTicker;
import java.util.OptionalLong;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.testkit.TestProbe;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.FailureEnvelope;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.messaging.MessageSlicer;
import org.opendaylight.yangtools.concepts.WritableIdentifier;
import scala.concurrent.duration.FiniteDuration;

/**
 * Test suite covering logic contained in {@link ConnectingClientConnection}. It assumes {@link ConnectionEntryTest}
 * passes.
 */
@RunWith(MockitoJUnitRunner.class)
public class ConnectingClientConnectionTest {
    private static class MockFailure extends RequestFailure<WritableIdentifier, MockFailure> {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        MockFailure(final WritableIdentifier target, final RequestException cause) {
            super(target, 0, cause);
        }

        @Override
        protected SerialForm<WritableIdentifier, MockFailure> externalizableProxy(final ABIVersion version) {
            return null;
        }

        @Override
        protected MockFailure cloneAsVersion(final ABIVersion version) {
            return this;
        }
    }

    private static class MockRequest extends Request<WritableIdentifier, MockRequest> {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        MockRequest(final WritableIdentifier target, final ActorRef replyTo) {
            super(target, 0, replyTo);
        }

        @Override
        public RequestFailure<WritableIdentifier, ?> toRequestFailure(final RequestException cause) {
            return new MockFailure(getTarget(), cause);
        }

        @Override
        protected Request.SerialForm<WritableIdentifier, MockRequest> externalizableProxy(final ABIVersion version) {
            return null;
        }

        @Override
        protected MockRequest cloneAsVersion(final ABIVersion version) {
            return this;
        }
    }

    @Mock
    private ActorRef mockReplyTo;
    @Mock
    private WritableIdentifier mockIdentifier;
    @Mock
    private RequestException mockCause;
    @Mock
    private Consumer<Response<?, ?>> mockCallback;
    @Mock
    private ClientActorBehavior<?> mockBehavior;
    @Mock
    private ClientActorContext mockContext;

    private FakeTicker ticker;
    private BackendInfo mockBackendInfo;
    private MockRequest mockRequest;
    private MockRequest mockRequest2;
    private RequestFailure<WritableIdentifier, ?> mockResponse;
    private FailureEnvelope mockResponseEnvelope;
    private Long mockCookie;

    private static ActorSystem actorSystem;
    private TestProbe mockActor;

    private AbstractClientConnection<?> queue;

    @BeforeClass
    public static void setupClass() {
        actorSystem = ActorSystem.apply();
    }

    @AfterClass
    public static void teardownClass() {
        actorSystem.terminate();
    }

    @Before
    public void setup() {
        doNothing().when(mockCallback).accept(any(MockFailure.class));

        ticker = new FakeTicker();
        ticker.advance(ThreadLocalRandom.current().nextLong());
        doReturn(ticker).when(mockContext).ticker();

        final var mockConfig = AccessClientUtil.newMockClientActorConfig();
        doReturn(mockConfig).when(mockContext).config();

        doReturn(mock(MessageSlicer.class)).when(mockContext).messageSlicer();

        mockActor = TestProbe.apply(actorSystem);
        mockBackendInfo = new BackendInfo(mockActor.ref(), "test", 0, ABIVersion.current(), 5);
        mockRequest = new MockRequest(mockIdentifier, mockReplyTo);
        mockRequest2 = new MockRequest(mockIdentifier, mockReplyTo);
        mockResponse = mockRequest.toRequestFailure(mockCause);
        mockResponseEnvelope = new FailureEnvelope(mockResponse, 0, 0, 0);
        mockCookie = ThreadLocalRandom.current().nextLong();

        queue = new ConnectingClientConnection<>(mockContext, mockCookie, mockBackendInfo.getName());
    }

    @After
    public void teardown() {
        actorSystem.stop(mockActor.ref());
    }

    @Test
    public void testCookie() {
        assertEquals(mockCookie, queue.cookie());
    }

    @Test
    public void testPoison() {
        queue.sendRequest(mockRequest, mockCallback);
        queue.poison(mockCause);

        final var captor = ArgumentCaptor.forClass(MockFailure.class);
        verify(mockCallback).accept(captor.capture());
        assertSame(mockCause, captor.getValue().getCause());
    }

    @Test
    public void testPoisonPerformsClose() {
        // Implies close()
        queue.poison(mockCause);

        // Kaboom
        final var ise = assertThrows(IllegalStateException.class, () -> queue.sendRequest(mockRequest, mockCallback));
        assertThat(ise.getMessage()).endsWith(" has been poisoned");
    }

    @Test
    public void testPoisonIdempotent() {
        queue.poison(mockCause);
        queue.poison(mockCause);
    }

    @Test
    public void testSendRequestNeedsBackend() {
        queue.sendRequest(mockRequest, mockCallback);
        final var ret = queue.checkTimeout(ticker.read());
        assertNotNull(ret);
        assertTrue(ret.isPresent());
    }

    @Test
    public void testSetBackendWithNoRequests() {
        // this utility method covers the entire test
        setupBackend();
    }

    @Test
    public void testSendRequestNeedsTimer() {
        setupBackend();

        queue.sendRequest(mockRequest, mockCallback);
        final var ret = queue.checkTimeout(ticker.read());
        assertNotNull(ret);
        assertTrue(ret.isPresent());
        assertTransmit(mockRequest, 0);
    }

    @Test
    public void testRunTimeoutEmpty() {
        var ret = queue.checkTimeout(ticker.read());
        assertNotNull(ret);
        assertFalse(ret.isPresent());
    }

    @Test
    public void testRunTimeoutWithoutShift() {
        queue.sendRequest(mockRequest, mockCallback);
        var ret = queue.checkTimeout(ticker.read());
        assertNotNull(ret);
        assertTrue(ret.isPresent());
    }

    @Test
    public void testRunTimeoutWithTimeoutLess() {
        queue.sendRequest(mockRequest, mockCallback);

        ticker.advance(AbstractClientConnection.DEFAULT_BACKEND_ALIVE_TIMEOUT_NANOS - 1);

        var ret = queue.checkTimeout(ticker.read());
        assertNotNull(ret);
        assertTrue(ret.isPresent());
    }

    @Test
    public void testRunTimeoutWithTimeoutExact() {
        setupBackend();

        queue.sendRequest(mockRequest, mockCallback);

        ticker.advance(AbstractClientConnection.DEFAULT_BACKEND_ALIVE_TIMEOUT_NANOS);

        var ret = queue.checkTimeout(ticker.read());
        assertNull(ret);
    }

    @Test
    public void testRunTimeoutWithTimeoutMore() {
        setupBackend();

        queue.sendRequest(mockRequest, mockCallback);

        ticker.advance(AbstractClientConnection.DEFAULT_BACKEND_ALIVE_TIMEOUT_NANOS + 1);

        assertNull(queue.checkTimeout(ticker.read()));
    }

    @Test
    @Ignore
    public void testRunTimeoutWithoutProgressExact() {
        queue.sendRequest(mockRequest, mockCallback);

        ticker.advance(AbstractClientConnection.DEFAULT_NO_PROGRESS_TIMEOUT_NANOS);

        // Kaboom
        queue.runTimer((ClientActorBehavior) mockBehavior);
        assertNotNull(queue.poisoned());
    }

    @Test
    public void testRunTimeoutWithoutProgressMore() {
        queue.sendRequest(mockRequest, mockCallback);

        ticker.advance(AbstractClientConnection.DEFAULT_NO_PROGRESS_TIMEOUT_NANOS + 1);

        // Kaboom
        queue.runTimer((ClientActorBehavior) mockBehavior);
        assertNotNull(queue.poisoned());
    }

    @Test
    public void testRunTimeoutEmptyWithoutProgressExact() {
        ticker.advance(AbstractClientConnection.DEFAULT_NO_PROGRESS_TIMEOUT_NANOS);

        // No problem
        assertEquals(OptionalLong.empty(), queue.checkTimeout(ticker.read()));
    }

    @Test
    public void testRunTimeoutEmptyWithoutProgressMore() {
        ticker.advance(AbstractClientConnection.DEFAULT_NO_PROGRESS_TIMEOUT_NANOS + 1);

        // No problem
        assertEquals(OptionalLong.empty(), queue.checkTimeout(ticker.read()));
    }

    @Test
    public void testCompleteEmpty() {
        queue.receiveResponse(mockResponseEnvelope);
        verifyNoMoreInteractions(mockCallback);
    }

    @Test
    public void testCompleteSingle() {
        setupBackend();

        queue.sendRequest(mockRequest, mockCallback);

        queue.receiveResponse(mockResponseEnvelope);
        verify(mockCallback).accept(mockResponse);

        queue.receiveResponse(mockResponseEnvelope);
        verifyNoMoreInteractions(mockCallback);
    }

    @Test
    public void testCompleteNull() {
        setupBackend();

        queue.sendRequest(mockRequest, mockCallback);

        doNothing().when(mockCallback).accept(mockResponse);

        queue.receiveResponse(mockResponseEnvelope);
        verify(mockCallback).accept(mockResponse);
    }

    @Test
    public void testProgressRecord() {
        setupBackend();

        queue.sendRequest(mockRequest, mockCallback);

        ticker.advance(10);
        queue.sendRequest(mockRequest2, mockCallback);
        queue.receiveResponse(mockResponseEnvelope);

        ticker.advance(AbstractClientConnection.DEFAULT_NO_PROGRESS_TIMEOUT_NANOS - 11);

        assertNull(queue.checkTimeout(ticker.read()));
    }

    private void setupBackend() {
        final ConnectingClientConnection<BackendInfo> connectingConn =
                new ConnectingClientConnection<>(mockContext, mockCookie, "test");
        final ConnectedClientConnection<BackendInfo> connectedConn =
                new ConnectedClientConnection<>(connectingConn, mockBackendInfo);
        queue.setForwarder(new SimpleReconnectForwarder(connectedConn));
        queue = connectedConn;
    }

    private void assertTransmit(final Request<?, ?> expected, final long sequence) {
        assertTrue(mockActor.msgAvailable());
        assertRequestEquals(expected, sequence, mockActor.receiveOne(FiniteDuration.apply(5, TimeUnit.SECONDS)));
    }

    private static void assertRequestEquals(final Request<?, ?> expected, final long sequence, final Object obj) {
        final var actual = assertInstanceOf(RequestEnvelope.class, obj);
        assertEquals(0, actual.getSessionId());
        assertEquals(sequence, actual.getTxSequence());
        assertSame(expected, actual.getMessage());
    }
}
