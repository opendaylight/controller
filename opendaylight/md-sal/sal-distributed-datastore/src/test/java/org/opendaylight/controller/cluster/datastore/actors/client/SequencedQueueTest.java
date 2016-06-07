/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.TestProbe;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
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
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.common.actor.TestTicker;
import org.opendaylight.yangtools.concepts.WritableIdentifier;
import scala.concurrent.duration.FiniteDuration;

/**
 * Test suite covering logic contained in {@link SequencedQueue}. It assumes {@link SequencedQueueEntryTest} passes.
 *
 * @author Robert Varga
 */
public class SequencedQueueTest {
    private static class MockFailure extends RequestFailure<WritableIdentifier, MockFailure> {
        private static final long serialVersionUID = 1L;

        MockFailure(final WritableIdentifier target, final long sequence, final long retry, final RequestException cause) {
            super(target, sequence, retry, cause);
        }

        @Override
        protected AbstractRequestFailureProxy<WritableIdentifier, MockFailure> externalizableProxy(final ABIVersion version) {
            return null;
        }

        @Override
        protected MockFailure cloneAsVersion(final ABIVersion version) {
            return this;
        }
    }

    private static class MockRequest extends Request<WritableIdentifier, MockRequest> {
        private static final long serialVersionUID = 1L;

        MockRequest(final WritableIdentifier target, final long sequence, final ActorRef replyTo) {
            super(target, sequence, 0, replyTo);
        }


        MockRequest(final MockRequest request, final long retry) {
            super(request, retry);
        }

        @Override
        public RequestFailure<WritableIdentifier, ?> toRequestFailure(final RequestException cause) {
            return new MockFailure(getTarget(), getSequence(), getRetry(), cause);
        }

        @Override
        protected AbstractRequestProxy<WritableIdentifier, MockRequest> externalizableProxy(final ABIVersion version) {
            return null;
        }

        @Override
        protected MockRequest cloneAsVersion(final ABIVersion version) {
            return this;
        }

        @Override
        protected MockRequest cloneAsRetry(final long retry) {
            return new MockRequest(this, retry);
        }
    };

    @Mock
    private ActorRef mockReplyTo;
    @Mock
    private WritableIdentifier mockIdentifier;
    @Mock
    private RequestException mockCause;
    @Mock
    private RequestCallback mockCallback;
    @Mock
    private ClientActorBehavior mockBehavior;

    private TestTicker ticker;
    private BackendInfo mockBackendInfo;
    private MockRequest mockRequest;
    private MockRequest mockRequest2;
    private Response<WritableIdentifier, ?> mockResponse;

    private static ActorSystem actorSystem;
    private TestProbe mockActor;

    private SequencedQueue queue;

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

        doReturn(mockBehavior).when(mockCallback).complete(any(MockFailure.class));

        ticker = new TestTicker();
        ticker.increment(ThreadLocalRandom.current().nextLong());

        mockActor = TestProbe.apply(actorSystem);
        mockBackendInfo = new BackendInfo(mockActor.ref(), ABIVersion.current());
        mockRequest = new MockRequest(mockIdentifier, ThreadLocalRandom.current().nextLong(), mockReplyTo);
        mockRequest2 = new MockRequest(mockIdentifier, mockRequest.getSequence() + 1, mockReplyTo);
        mockResponse = mockRequest.toRequestFailure(mockCause);

        queue = new SequencedQueue(mockIdentifier, ticker);
    }

    @After
    public void teardown() {
        actorSystem.stop(mockActor.ref());
    }

    @Test
    public void testGetTarget() {
        assertSame(mockIdentifier, queue.getTarget());
    }

    @Test
    public void testEmptyClose() {
        assertFalse(queue.hasCompleted());
        queue.close();
        assertTrue(queue.hasCompleted());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testDuplicateEnqueueRequest() {
        // First entry
        queue.enqueueRequest(mockRequest, mockCallback);

        // Kaboom
        queue.enqueueRequest(mockRequest, mockCallback);
    }

    @Test(expected=IllegalStateException.class)
    public void testClosedEnqueueRequest() {
        queue.close();

        // Kaboom
        queue.enqueueRequest(mockRequest, mockCallback);
    }

    @Test
    public void testCloseIdempotent() {
        queue.close();
        queue.close();
    }

    @Test
    public void testPoison() {
        queue.enqueueRequest(mockRequest, mockCallback);
        queue.poison(mockCause);

        final ArgumentCaptor<MockFailure> captor = ArgumentCaptor.forClass(MockFailure.class);
        verify(mockCallback).complete(captor.capture());
        assertSame(mockCause, captor.getValue().getCause());
    }

    @Test(expected=IllegalStateException.class)
    public void testPoisonPerformsClose() {
        // Implies close()
        queue.poison(mockCause);

        // Kaboom
        queue.enqueueRequest(mockRequest, mockCallback);
    }

    @Test
    public void testPoisonIdempotent() {
        queue.poison(mockCause);
        queue.poison(mockCause);
    }

    @Test
    public void testEnqueueRequestNeedsBackend() {
        final Optional<FiniteDuration> ret = queue.enqueueRequest(mockRequest, mockCallback);

        assertNotNull(ret);
        assertFalse(ret.isPresent());
    }

    @Test
    public void testExpectProof() {
        final CompletableFuture<BackendInfo> proof = new CompletableFuture<>();
        assertTrue(queue.expectProof(proof));
        assertFalse(queue.expectProof(proof));
    }

    @Test(expected=NullPointerException.class)
    public void testSetBackendNull() {
        final CompletableFuture<BackendInfo> proof = new CompletableFuture<>();
        assertTrue(queue.expectProof(proof));
        queue.setBackendInfo(proof, null);
    }

    @Test
    public void testSetBackendWithNoResolution() {
        queue.enqueueRequest(mockRequest, mockCallback);

        final CompletableFuture<BackendInfo> proof = new CompletableFuture<>();
        final Optional<FiniteDuration> ret = queue.setBackendInfo(proof, mockBackendInfo);
        assertNotNull(ret);
        assertFalse(ret.isPresent());
    }

    @Test
    public void testSetBackendWithWrongProof() {
        queue.enqueueRequest(mockRequest, mockCallback);

        final CompletableFuture<BackendInfo> proof = new CompletableFuture<>();
        assertTrue(queue.expectProof(proof));

        final Optional<FiniteDuration> ret = queue.setBackendInfo(new CompletableFuture<>(), mockBackendInfo);
        assertNotNull(ret);
        assertFalse(ret.isPresent());
    }

    @Test
    public void testSetBackendWithNoRequests() {
        // this utility method covers the entire test
        setupBackend();
    }

    @Test
    public void testSetbackedWithRequestsNoTimer() {
        queue.enqueueRequest(mockRequest, mockCallback);

        final CompletableFuture<BackendInfo> proof = new CompletableFuture<>();
        assertTrue(queue.expectProof(proof));
        assertFalse(mockActor.msgAvailable());

        final Optional<FiniteDuration> ret = queue.setBackendInfo(proof, mockBackendInfo);
        assertNotNull(ret);
        assertTrue(ret.isPresent());

        assertTransmit(mockRequest);
    }

    @Test
    public void testEnqueueRequestNeedsTimer() {
        setupBackend();

        final Optional<FiniteDuration> ret = queue.enqueueRequest(mockRequest, mockCallback);
        assertNotNull(ret);
        assertTrue(ret.isPresent());
        assertTransmit(mockRequest);
    }

    @Test
    public void testEnqueueRequestWithoutTimer() {
        setupBackend();

        // First request
        Optional<FiniteDuration> ret = queue.enqueueRequest(mockRequest, mockCallback);
        assertNotNull(ret);
        assertTrue(ret.isPresent());
        assertTransmit(mockRequest);

        // Second request, no timer fired
        ret = queue.enqueueRequest(mockRequest2, mockCallback);
        assertNull(ret);
        assertTransmit(mockRequest2);
    }

    @Test
    public void testRunTimeoutEmpty() throws NoProgressException {
        final boolean ret = queue.runTimeout();
        assertFalse(ret);
    }

    @Test
    public void testRunTimeoutWithoutShift() throws NoProgressException {
        queue.enqueueRequest(mockRequest, mockCallback);
        final boolean ret = queue.runTimeout();
        assertFalse(ret);
    }

    @Test
    public void testRunTimeoutWithTimeoutLess() throws NoProgressException {
        queue.enqueueRequest(mockRequest, mockCallback);

        ticker.increment(SequencedQueue.REQUEST_TIMEOUT_NANOS - 1);

        final boolean ret = queue.runTimeout();
        assertFalse(ret);
    }

    @Test
    public void testRunTimeoutWithTimeoutExact() throws NoProgressException {
        queue.enqueueRequest(mockRequest, mockCallback);

        ticker.increment(SequencedQueue.REQUEST_TIMEOUT_NANOS);

        final boolean ret = queue.runTimeout();
        assertTrue(ret);
    }

    @Test
    public void testRunTimeoutWithTimeoutMore() throws NoProgressException {
        queue.enqueueRequest(mockRequest, mockCallback);

        ticker.increment(SequencedQueue.REQUEST_TIMEOUT_NANOS + 1);

        final boolean ret = queue.runTimeout();
        assertTrue(ret);
    }

    @Test(expected=NoProgressException.class)
    public void testRunTimeoutWithoutProgressExact() throws NoProgressException {
        queue.enqueueRequest(mockRequest, mockCallback);

        ticker.increment(SequencedQueue.NO_PROGRESS_TIMEOUT_NANOS);

        // Kaboom
        queue.runTimeout();
    }

    @Test(expected=NoProgressException.class)
    public void testRunTimeoutWithoutProgressMore() throws NoProgressException {
        queue.enqueueRequest(mockRequest, mockCallback);

        ticker.increment(SequencedQueue.NO_PROGRESS_TIMEOUT_NANOS + 1);

        // Kaboom
        queue.runTimeout();
    }

    @Test
    public void testRunTimeoutEmptyWithoutProgressExact() throws NoProgressException {
        ticker.increment(SequencedQueue.NO_PROGRESS_TIMEOUT_NANOS);

        // No problem
        final boolean ret = queue.runTimeout();
        assertFalse(ret);
    }

    @Test
    public void testRunTimeoutEmptyWithoutProgressMore() throws NoProgressException {
        ticker.increment(SequencedQueue.NO_PROGRESS_TIMEOUT_NANOS + 1);

        // No problem
        final boolean ret = queue.runTimeout();
        assertFalse(ret);
    }

    @Test
    public void testCompleteEmpty() {
        final ClientActorBehavior ret = queue.complete(mockBehavior, mockResponse);
        assertSame(mockBehavior, ret);
        verifyNoMoreInteractions(mockCallback);
    }

    @Test
    public void testCompleteSingle() {
        queue.enqueueRequest(mockRequest, mockCallback);

        ClientActorBehavior ret = queue.complete(mockBehavior, mockResponse);
        verify(mockCallback).complete(mockResponse);
        assertSame(mockBehavior, ret);

        ret = queue.complete(mockBehavior, mockResponse);
        assertSame(mockBehavior, ret);
        verifyNoMoreInteractions(mockCallback);
    }

    @Test
    public void testCompleteNull() {
        queue.enqueueRequest(mockRequest, mockCallback);

        doReturn(null).when(mockCallback).complete(mockResponse);

        ClientActorBehavior ret = queue.complete(mockBehavior, mockResponse);
        verify(mockCallback).complete(mockResponse);
        assertNull(ret);
    }

    @Test
    public void testProgressRecord() throws NoProgressException {
        setupBackend();

        queue.enqueueRequest(mockRequest, mockCallback);

        ticker.increment(10);
        queue.enqueueRequest(mockRequest2, mockCallback);
        queue.complete(mockBehavior, mockResponse);

        ticker.increment(SequencedQueue.NO_PROGRESS_TIMEOUT_NANOS - 11);
        assertTrue(queue.runTimeout());
    }

    private void setupBackend() {
        final CompletableFuture<BackendInfo> proof = new CompletableFuture<>();
        assertTrue(queue.expectProof(proof));
        final Optional<FiniteDuration> ret = queue.setBackendInfo(proof, mockBackendInfo);
        assertNotNull(ret);
        assertFalse(ret.isPresent());
        assertFalse(mockActor.msgAvailable());
    }

    private void assertTransmit(final Request<?, ?> expected) {
        assertTrue(mockActor.msgAvailable());
        assertRequestEquals(expected, mockActor.receiveOne(FiniteDuration.apply(5, TimeUnit.SECONDS)));
    }

    private static void assertRequestEquals(final Request<?, ?> expected, final Object o) {
        final Request<?, ?> actual = (Request<?, ?>) o;
        assertEquals(expected.getRetry(), actual.getRetry());
        assertEquals(expected.getSequence(), actual.getSequence());
        assertEquals(expected.getTarget(), actual.getTarget());
    }
}
