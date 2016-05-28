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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.TestProbe;
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
import scala.concurrent.duration.Duration;

/**
 * Test suite covering logic contained in {@link SequencedQueueEntry}.
 *
 * @author Robert Varga
 */
public class SequencedQueueEntryTest {
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
    private Request<WritableIdentifier, ?> mockRequest;
    private Response<WritableIdentifier, ?> mockResponse;

    private static ActorSystem actorSystem;
    private TestProbe mockActor;

    private SequencedQueueEntry entry;

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
        mockResponse = mockRequest.toRequestFailure(mockCause);

        entry = new SequencedQueueEntry(mockRequest, mockCallback, ticker.read());
    }

    @After
    public void teardown() {
        actorSystem.stop(mockActor.ref());
    }

    @Test
    public void testGetSequence() {
        assertEquals(mockRequest.getSequence(), entry.getSequence());
    }

    @Test
    public void testGetCurrentTry() {
        assertEquals(0, entry.getCurrentTry());
        entry.retransmit(mockBackendInfo, ticker.read());
        assertEquals(0, entry.getCurrentTry());
        entry.retransmit(mockBackendInfo, ticker.read());
        assertEquals(1, entry.getCurrentTry());
        entry.retransmit(mockBackendInfo, ticker.read());
        assertEquals(2, entry.getCurrentTry());
    }

    @Test
    public void testComplete() {
        entry.complete(mockResponse);
        verify(mockCallback).complete(mockResponse);
    }

    @Test
    public void testPoison() {
        entry.poison(mockCause);

        final ArgumentCaptor<MockFailure> captor = ArgumentCaptor.forClass(MockFailure.class);
        verify(mockCallback).complete(captor.capture());
        assertSame(mockCause, captor.getValue().getCause());
    }

    @Test
    public void testIsTimedOut() {
        assertTrue(entry.isTimedOut(ticker.read(), 0));
        assertFalse(entry.isTimedOut(ticker.read(), 1));

        entry.retransmit(mockBackendInfo, ticker.read());
        assertTrue(entry.isTimedOut(ticker.read(), 0));
        ticker.increment(10);
        assertTrue(entry.isTimedOut(ticker.read(), 10));
        assertFalse(entry.isTimedOut(ticker.read(), 20));

        entry.retransmit(mockBackendInfo, ticker.read());
        assertTrue(entry.isTimedOut(ticker.read(), 0));
        ticker.increment(10);
        assertTrue(entry.isTimedOut(ticker.read(), 10));
        assertFalse(entry.isTimedOut(ticker.read(), 11));
    }

    @Test
    public void testRetransmit() {
        assertFalse(mockActor.msgAvailable());
        entry.retransmit(mockBackendInfo, ticker.read());

        assertTrue(mockActor.msgAvailable());
        assertRequestEquals(mockRequest, mockActor.receiveOne(Duration.apply(5, TimeUnit.SECONDS)));
    }

     private static void assertRequestEquals(final Request<?, ?> expected, final Object o) {
         final Request<?, ?> actual = (Request<?, ?>) o;
         assertEquals(expected.getRetry(), actual.getRetry());
         assertEquals(expected.getSequence(), actual.getSequence());
         assertEquals(expected.getTarget(), actual.getTarget());
    }
}
