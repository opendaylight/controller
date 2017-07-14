/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.TestProbe;
import com.google.common.testing.FakeTicker;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.AbstractRequestFailureProxy;
import org.opendaylight.controller.cluster.access.concepts.AbstractRequestProxy;
import org.opendaylight.controller.cluster.access.concepts.FailureEnvelope;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.yangtools.concepts.WritableIdentifier;
import scala.concurrent.duration.FiniteDuration;

/**
 * Test suite covering logic contained in {@link ConnectingClientConnection}. It assumes {@link ConnectionEntryTest}
 * passes.
 *
 * @author Robert Varga
 */
public class ConnectingClientConnectionTest {
    private static class MockFailure extends RequestFailure<WritableIdentifier, MockFailure> {
        private static final long serialVersionUID = 1L;

        MockFailure(final WritableIdentifier target, final RequestException cause) {
            super(target, 0, cause);
        }

        @Override
        protected AbstractRequestFailureProxy<WritableIdentifier, MockFailure> externalizableProxy(
                final ABIVersion version) {
            return null;
        }

        @Override
        protected MockFailure cloneAsVersion(final ABIVersion version) {
            return this;
        }
    }

    private static class MockRequest extends Request<WritableIdentifier, MockRequest> {
        private static final long serialVersionUID = 1L;

        MockRequest(final WritableIdentifier target, final ActorRef replyTo) {
            super(target, 0, replyTo);
        }

        @Override
        public RequestFailure<WritableIdentifier, ?> toRequestFailure(final RequestException cause) {
            return new MockFailure(getTarget(), cause);
        }

        @Override
        protected AbstractRequestProxy<WritableIdentifier, MockRequest> externalizableProxy(final ABIVersion version) {
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
        MockitoAnnotations.initMocks(this);

        doNothing().when(mockCallback).accept(any(MockFailure.class));

        ticker = new FakeTicker();
        ticker.advance(ThreadLocalRandom.current().nextLong());
        doReturn(ticker).when(mockContext).ticker();

        mockActor = TestProbe.apply(actorSystem);
        mockBackendInfo = new BackendInfo(mockActor.ref(), 0, ABIVersion.current(), 5);
        mockRequest = new MockRequest(mockIdentifier, mockReplyTo);
        mockRequest2 = new MockRequest(mockIdentifier, mockReplyTo);
        mockResponse = mockRequest.toRequestFailure(mockCause);
        mockResponseEnvelope = new FailureEnvelope(mockResponse, 0, 0, 0);
        mockCookie = ThreadLocalRandom.current().nextLong();

        queue = new ConnectingClientConnection<>(mockContext, mockCookie);
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

        final ArgumentCaptor<MockFailure> captor = ArgumentCaptor.forClass(MockFailure.class);
        verify(mockCallback).accept(captor.capture());
        assertSame(mockCause, captor.getValue().getCause());
    }

    @Test(expected = IllegalStateException.class)
    public void testPoisonPerformsClose() {
        // Implies close()
        queue.poison(mockCause);

        // Kaboom
        queue.sendRequest(mockRequest, mockCallback);
    }

    @Test
    public void testPoisonIdempotent() {
        queue.poison(mockCause);
        queue.poison(mockCause);
    }

    @Test
    public void testSendRequestNeedsBackend() {
        queue.sendRequest(mockRequest, mockCallback);
        final Optional<Long> ret = queue.checkTimeout(ticker.read());
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
        final Optional<Long> ret = queue.checkTimeout(ticker.read());
        assertNotNull(ret);
        assertTrue(ret.isPresent());
        assertTransmit(mockRequest, 0);
    }

    @Test
    public void testRunTimeoutEmpty() throws NoProgressException {
        Optional<Long> ret = queue.checkTimeout(ticker.read());
        assertNotNull(ret);
        assertFalse(ret.isPresent());
    }

    @Test
    public void testRunTimeoutWithoutShift() throws NoProgressException {
        queue.sendRequest(mockRequest, mockCallback);
        Optional<Long> ret = queue.checkTimeout(ticker.read());
        assertNotNull(ret);
        assertTrue(ret.isPresent());
    }

    @Test
    public void testRunTimeoutWithTimeoutLess() throws NoProgressException {
        queue.sendRequest(mockRequest, mockCallback);

        ticker.advance(AbstractClientConnection.BACKEND_ALIVE_TIMEOUT_NANOS - 1);

        Optional<Long> ret = queue.checkTimeout(ticker.read());
        assertNotNull(ret);
        assertTrue(ret.isPresent());
    }

    @Test
    public void testRunTimeoutWithTimeoutExact() throws NoProgressException {
        setupBackend();

        queue.sendRequest(mockRequest, mockCallback);

        ticker.advance(AbstractClientConnection.BACKEND_ALIVE_TIMEOUT_NANOS);

        Optional<Long> ret = queue.checkTimeout(ticker.read());
        assertNull(ret);
    }

    @Test
    public void testRunTimeoutWithTimeoutMore() throws NoProgressException {
        setupBackend();

        queue.sendRequest(mockRequest, mockCallback);

        ticker.advance(AbstractClientConnection.BACKEND_ALIVE_TIMEOUT_NANOS + 1);

        Optional<Long> ret = queue.checkTimeout(ticker.read());
        assertNull(ret);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testRunTimeoutWithoutProgressExact() throws NoProgressException {
        queue.sendRequest(mockRequest, mockCallback);

        ticker.advance(AbstractClientConnection.NO_PROGRESS_TIMEOUT_NANOS);

        // Kaboom
        queue.runTimer((ClientActorBehavior) mockBehavior);
        assertNotNull(queue.poisoned());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testRunTimeoutWithoutProgressMore() throws NoProgressException {
        queue.sendRequest(mockRequest, mockCallback);

        ticker.advance(AbstractClientConnection.NO_PROGRESS_TIMEOUT_NANOS + 1);

        // Kaboom
        queue.runTimer((ClientActorBehavior) mockBehavior);
        assertNotNull(queue.poisoned());
    }

    @Test
    public void testRunTimeoutEmptyWithoutProgressExact() throws NoProgressException {
        ticker.advance(AbstractClientConnection.NO_PROGRESS_TIMEOUT_NANOS);

        // No problem
        Optional<Long> ret = queue.checkTimeout(ticker.read());
        assertNotNull(ret);
        assertFalse(ret.isPresent());
    }

    @Test
    public void testRunTimeoutEmptyWithoutProgressMore() throws NoProgressException {
        ticker.advance(AbstractClientConnection.NO_PROGRESS_TIMEOUT_NANOS + 1);

        // No problem
        Optional<Long> ret = queue.checkTimeout(ticker.read());
        assertNotNull(ret);
        assertFalse(ret.isPresent());
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
    public void testProgressRecord() throws NoProgressException {
        setupBackend();

        queue.sendRequest(mockRequest, mockCallback);

        ticker.advance(10);
        queue.sendRequest(mockRequest2, mockCallback);
        queue.receiveResponse(mockResponseEnvelope);

        ticker.advance(AbstractClientConnection.NO_PROGRESS_TIMEOUT_NANOS - 11);

        Optional<Long> ret = queue.checkTimeout(ticker.read());
        assertNull(ret);
    }

    private void setupBackend() {
        final ConnectingClientConnection<BackendInfo> connectingConn =
                new ConnectingClientConnection<>(mockContext, mockCookie);
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
        assertTrue(obj instanceof RequestEnvelope);

        final RequestEnvelope actual = (RequestEnvelope) obj;
        assertEquals(0, actual.getSessionId());
        assertEquals(sequence, actual.getTxSequence());
        assertSame(expected, actual.getMessage());
    }
}
