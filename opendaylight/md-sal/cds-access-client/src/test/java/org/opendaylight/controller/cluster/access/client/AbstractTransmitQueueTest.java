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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.opendaylight.controller.cluster.access.client.ConnectionEntryMatcher.entryWithRequest;

import com.google.common.base.Ticker;
import java.util.function.Consumer;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.testkit.TestProbe;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.controller.cluster.access.commands.TransactionPurgeRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionPurgeResponse;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.SuccessEnvelope;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

abstract class AbstractTransmitQueueTest<T extends TransmitQueue> {
    private static final FrontendIdentifier FRONTEND =
            FrontendIdentifier.create(MemberName.forName("test"), FrontendType.forName("type-1"));
    private static final ClientIdentifier CLIENT = ClientIdentifier.create(FRONTEND, 0);

    static final LocalHistoryIdentifier HISTORY = new LocalHistoryIdentifier(CLIENT, 0);
    static final TransactionIdentifier TRANSACTION_IDENTIFIER = new TransactionIdentifier(HISTORY, 0);

    T queue;
    ActorSystem system;
    TestProbe probe;

    @BeforeEach
    final void beforeEach() {
        system = ActorSystem.apply();
        probe = new TestProbe(system);
        queue = createQueue();
    }

    abstract int getMaxInFlightMessages();

    abstract T createQueue();

    @AfterEach
    final void afterEach() {
        TestKit.shutdownActorSystem(system);
    }

    @Test
    abstract void testCanTransmitCount();

    @Test
    abstract void testTransmit();

    @Test
    final void testAsIterable() {
        final var request = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 0L, probe.ref());
        final var callback = createConsumerMock();
        final long now = Ticker.systemTicker().read();
        final int sentMessages = getMaxInFlightMessages() + 1;
        for (int i = 0; i < sentMessages; i++) {
            queue.enqueueOrForward(new ConnectionEntry(request, callback, now), now);
        }
        final var entries = queue.drain();
        assertEquals(sentMessages, entries.size());
        assertThat(entries, everyItem(entryWithRequest(request)));
    }

    @Test
    final void testTicksStalling() {
        final long now = Ticker.systemTicker().read();
        assertEquals(0, queue.ticksStalling(now));
    }

    @Test
    final void testCompleteReponseNotMatchingRequest() {
        final long requestSequence = 0L;
        final long txSequence = 0L;
        final long sessionId = 0L;
        final var request = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, requestSequence, probe.ref());
        final var callback = createConsumerMock();
        final long now = Ticker.systemTicker().read();
        queue.enqueueOrForward(new ConnectionEntry(request, callback, now), now);
        //different transaction id
        final var anotherTxId = new TransactionIdentifier(HISTORY, 1L);
        final var success1 = new TransactionPurgeResponse(anotherTxId, requestSequence);
        assertNull(queue.complete(new SuccessEnvelope(success1, sessionId, txSequence, 1L), now));

        //different response sequence
        final long differentResponseSequence = 1L;
        final var success2 = new TransactionPurgeResponse(TRANSACTION_IDENTIFIER, differentResponseSequence);
        assertNull(queue.complete(new SuccessEnvelope(success2, sessionId, txSequence, 1L), now));

        //different tx sequence
        final long differentTxSequence = 1L;
        final var success3 = new TransactionPurgeResponse(TRANSACTION_IDENTIFIER, requestSequence);
        assertNull(queue.complete(new SuccessEnvelope(success3, sessionId, differentTxSequence, 1L), now));

        //different session id
        final long differentSessionId = 1L;
        final var success4 = new TransactionPurgeResponse(TRANSACTION_IDENTIFIER, requestSequence);
        assertNull(queue.complete(new SuccessEnvelope(success4, differentSessionId, differentTxSequence, 1L), now));
    }

    @Test
    final void testIsEmpty() {
        assertTrue(queue.isEmpty());
        final Request<?, ?> request = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 0L, probe.ref());
        final Consumer<Response<?, ?>> callback = createConsumerMock();
        final long now = Ticker.systemTicker().read();
        queue.enqueueOrForward(new ConnectionEntry(request, callback, now), now);
        assertFalse(queue.isEmpty());
    }

    @Test
    final void testPeek() {
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
    final  void testPoison() {
        final Request<?, ?> request = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 0L, probe.ref());
        final Consumer<Response<?, ?>> callback = createConsumerMock();
        final long now = Ticker.systemTicker().read();
        queue.enqueueOrForward(new ConnectionEntry(request, callback, now), now);
        assertEquals(1, queue.poison().size());
    }

    @SuppressWarnings("unchecked")
    static final Consumer<Response<?, ?>> createConsumerMock() {
        return mock(Consumer.class);
    }
}
