/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.opendaylight.controller.cluster.access.client.ConnectionEntryMatcher.entryWithRequest;

import akka.actor.ActorSystem;
import akka.testkit.TestProbe;
import akka.testkit.javadsl.TestKit;
import com.google.common.base.Ticker;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.commands.TransactionPurgeRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionPurgeResponse;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestSuccess;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.SuccessEnvelope;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

public abstract class AbstractTransmitQueueTest<T extends TransmitQueue> {

    private static final FrontendIdentifier FRONTEND =
            FrontendIdentifier.create(MemberName.forName("test"), FrontendType.forName("type-1"));
    private static final ClientIdentifier CLIENT = ClientIdentifier.create(FRONTEND, 0);
    protected static final LocalHistoryIdentifier HISTORY = new LocalHistoryIdentifier(CLIENT, 0);
    protected static final TransactionIdentifier TRANSACTION_IDENTIFIER = new TransactionIdentifier(HISTORY, 0);
    protected T queue;
    protected ActorSystem system;
    protected TestProbe probe;

    protected abstract int getMaxInFlightMessages();

    protected abstract T createQueue();

    @Before
    public void setUp() {
        system = ActorSystem.apply();
        probe = new TestProbe(system);
        queue = createQueue();
    }

    @After
    public void tearDown() {
        TestKit.shutdownActorSystem(system);
    }

    @Test
    public abstract void testCanTransmitCount();

    @Test(expected = UnsupportedOperationException.class)
    public abstract void testTransmit();

    @Test
    public void testAsIterable() {
        final Request<?, ?> request = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 0L, probe.ref());
        final Consumer<Response<?, ?>> callback = createConsumerMock();
        final long now = Ticker.systemTicker().read();
        final int sentMessages = getMaxInFlightMessages() + 1;
        for (int i = 0; i < sentMessages; i++) {
            queue.enqueueOrForward(new ConnectionEntry(request, callback, now), now);
        }
        final Collection<ConnectionEntry> entries = queue.drain();
        assertEquals(sentMessages, entries.size());
        assertThat(entries, everyItem(entryWithRequest(request)));
    }

    @Test
    public void testTicksStalling() {
        final long now = Ticker.systemTicker().read();
        assertEquals(0, queue.ticksStalling(now));
    }

    @Test
    public void testCompleteReponseNotMatchingRequest() {
        final long requestSequence = 0L;
        final long txSequence = 0L;
        final long sessionId = 0L;
        final Request<?, ?> request = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, requestSequence, probe.ref());
        final Consumer<Response<?, ?>> callback = createConsumerMock();
        final long now = Ticker.systemTicker().read();
        queue.enqueueOrForward(new ConnectionEntry(request, callback, now), now);
        //different transaction id
        final TransactionIdentifier anotherTxId = new TransactionIdentifier(HISTORY, 1L);
        final RequestSuccess<?, ?> success1 = new TransactionPurgeResponse(anotherTxId, requestSequence);
        final Optional<TransmittedConnectionEntry> completed1 =
                queue.complete(new SuccessEnvelope(success1, sessionId, txSequence, 1L), now);
        assertFalse(completed1.isPresent());
        //different response sequence
        final long differentResponseSequence = 1L;
        final RequestSuccess<?, ?> success2 =
                new TransactionPurgeResponse(TRANSACTION_IDENTIFIER, differentResponseSequence);
        final Optional<TransmittedConnectionEntry> completed2 =
                queue.complete(new SuccessEnvelope(success2, sessionId, txSequence, 1L), now);
        assertFalse(completed2.isPresent());
        //different tx sequence
        final long differentTxSequence = 1L;
        final RequestSuccess<?, ?> success3 =
                new TransactionPurgeResponse(TRANSACTION_IDENTIFIER, requestSequence);
        final Optional<TransmittedConnectionEntry> completed3 =
                queue.complete(new SuccessEnvelope(success3, sessionId, differentTxSequence, 1L), now);
        assertFalse(completed3.isPresent());
        //different session id
        final long differentSessionId = 1L;
        final RequestSuccess<?, ?> success4 =
                new TransactionPurgeResponse(TRANSACTION_IDENTIFIER, requestSequence);
        final Optional<TransmittedConnectionEntry> completed4 =
                queue.complete(new SuccessEnvelope(success4, differentSessionId, differentTxSequence, 1L), now);
        assertFalse(completed4.isPresent());
    }

    @Test
    public void testIsEmpty() {
        assertTrue(queue.isEmpty());
        final Request<?, ?> request = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 0L, probe.ref());
        final Consumer<Response<?, ?>> callback = createConsumerMock();
        final long now = Ticker.systemTicker().read();
        queue.enqueueOrForward(new ConnectionEntry(request, callback, now), now);
        assertFalse(queue.isEmpty());
    }

    @Test
    public void testPeek() {
        final Request<?, ?> request1 = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 0L, probe.ref());
        final Request<?, ?> request2 = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 1L, probe.ref());
        final Consumer<Response<?, ?>> callback = createConsumerMock();
        final long now = Ticker.systemTicker().read();
        final ConnectionEntry entry1 = new ConnectionEntry(request1, callback, now);
        final ConnectionEntry entry2 = new ConnectionEntry(request2, callback, now);
        queue.enqueueOrForward(entry1, now);
        queue.enqueueOrForward(entry2, now);
        assertEquals(entry1.getRequest(), queue.peek().getRequest());
    }

    @Test
    public void testPoison() {
        final Request<?, ?> request = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 0L, probe.ref());
        final Consumer<Response<?, ?>> callback = createConsumerMock();
        final long now = Ticker.systemTicker().read();
        queue.enqueueOrForward(new ConnectionEntry(request, callback, now), now);
        assertEquals(1, queue.poison().size());
    }

    @SuppressWarnings("unchecked")
    protected static Consumer<Response<?, ?>> createConsumerMock() {
        return mock(Consumer.class);
    }
}
