/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static org.hamcrest.CoreMatchers.everyItem;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.opendaylight.controller.cluster.access.client.ConnectionEntryMatcher.entryWithRequest;

import com.google.common.base.Ticker;
import com.google.common.collect.Iterables;
import com.google.common.testing.FakeTicker;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.commands.TransactionPurgeRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionPurgeResponse;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RequestSuccess;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.SuccessEnvelope;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

public class TransmittingTransmitQueueTest extends AbstractTransmitQueueTest<TransmitQueue.Transmitting> {

    private BackendInfo backendInfo;

    @Override
    protected int getMaxInFlightMessages() {
        return backendInfo.getMaxMessages();
    }

    @Override
    protected TransmitQueue.Transmitting createQueue() {
        backendInfo = new BackendInfo(probe.ref(), 0L, ABIVersion.BORON, 3);
        return new TransmitQueue.Transmitting(0, backendInfo);
    }

    @Test
    public void testComplete() throws Exception {
        final long sequence1 = 0L;
        final long sequence2 = 1L;
        final Request request1 = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, sequence1, probe.ref());
        final TransactionIdentifier transactionIdentifier2 = new TransactionIdentifier(HISTORY, 1L);
        final Request request2 = new TransactionPurgeRequest(transactionIdentifier2, sequence2, probe.ref());
        final Consumer<Response<?, ?>> callback1 = createConsumerMock();
        final Consumer<Response<?, ?>> callback2 = createConsumerMock();
        final long now1 = Ticker.systemTicker().read();
        final long now2 = Ticker.systemTicker().read();
        //enqueue 2 entries
        queue.enqueue(new ConnectionEntry(request1, callback1, now1), now1);
        queue.enqueue(new ConnectionEntry(request2, callback2, now2), now2);
        final RequestSuccess<?, ?> success1 = new TransactionPurgeResponse(TRANSACTION_IDENTIFIER, sequence1);
        final RequestSuccess<?, ?> success2 = new TransactionPurgeResponse(transactionIdentifier2, sequence2);
        //complete entries in different order
        final Optional<TransmittedConnectionEntry> completed2 =
                queue.complete(new SuccessEnvelope(success2, 0L, sequence2, 1L), now2);
        final Optional<TransmittedConnectionEntry> completed1 =
                queue.complete(new SuccessEnvelope(success1, 0L, sequence1, 1L), now1);
        //check first entry
        final TransmittedConnectionEntry transmittedEntry1 = completed1.orElseThrow(AssertionError::new);
        Assert.assertEquals(transmittedEntry1.getRequest(), request1);
        Assert.assertEquals(transmittedEntry1.getTxSequence(), sequence1);
        Assert.assertEquals(transmittedEntry1.getCallback(), callback1);
        //check second entry
        final TransmittedConnectionEntry transmittedEntry2 = completed2.orElseThrow(AssertionError::new);
        Assert.assertEquals(transmittedEntry2.getRequest(), request2);
        Assert.assertEquals(transmittedEntry2.getTxSequence(), sequence2);
        Assert.assertEquals(transmittedEntry2.getCallback(), callback2);
    }

    @Test
    public void testEnqueueCanTransmit() throws Exception {
        final Request request = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 0L, probe.ref());
        final Consumer<Response<?, ?>> callback = createConsumerMock();
        final long now = Ticker.systemTicker().read();
        queue.enqueue(new ConnectionEntry(request, callback, now), now);
        final RequestEnvelope requestEnvelope = probe.expectMsgClass(RequestEnvelope.class);
        Assert.assertEquals(request, requestEnvelope.getMessage());
    }

    @Test
    public void testEnqueueBackendFull() throws Exception {
        final Request request = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 0L, probe.ref());
        final Consumer<Response<?, ?>> callback = createConsumerMock();
        final long now = Ticker.systemTicker().read();
        final int sentMessages = getMaxInFlightMessages() + 1;
        for (int i = 0; i < sentMessages; i++) {
            queue.enqueue(new ConnectionEntry(request, callback, now), now);
        }
        for (int i = 0; i < getMaxInFlightMessages(); i++) {
            probe.expectMsgClass(RequestEnvelope.class);
        }
        probe.expectNoMsg();
        final Iterable<ConnectionEntry> entries = queue.asIterable();
        Assert.assertEquals(sentMessages, Iterables.size(entries));
        Assert.assertThat(entries, everyItem(entryWithRequest(request)));
    }

    @Test
    @Override
    public void testCanTransmitCount() throws Exception {
        Assert.assertTrue(queue.canTransmitCount(getMaxInFlightMessages() - 1) > 0);
        Assert.assertFalse(queue.canTransmitCount(getMaxInFlightMessages()) > 0);
    }

    @Test
    @Override
    public void testTransmit() throws Exception {
        final Request request = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 0L, probe.ref());
        final Consumer<Response<?, ?>> callback = createConsumerMock();
        final long now = Ticker.systemTicker().read();
        final ConnectionEntry entry = new ConnectionEntry(request, callback, now);
        queue.transmit(entry, now);
        final RequestEnvelope requestEnvelope = probe.expectMsgClass(RequestEnvelope.class);
        Assert.assertEquals(request, requestEnvelope.getMessage());
    }

    @Test
    public void testSetForwarder() throws Exception {
        final FakeTicker ticker = new FakeTicker();
        ticker.setAutoIncrementStep(1, TimeUnit.MICROSECONDS);
        final Request request = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 0L, probe.ref());
        final Consumer<Response<?, ?>> callback = createConsumerMock();
        final ConnectionEntry entry = new ConnectionEntry(request, callback, ticker.read());
        queue.enqueue(entry, ticker.read());
        final ReconnectForwarder forwarder = mock(ReconnectForwarder.class);
        final long setForwarderNow = ticker.read();
        queue.setForwarder(forwarder, setForwarderNow);
        verify(forwarder).forwardEntry(isA(TransmittedConnectionEntry.class), eq(setForwarderNow));
        final long secondEnqueueNow = ticker.read();
        queue.enqueue(entry, secondEnqueueNow);
        verify(forwarder).forwardEntry(entry, secondEnqueueNow);
    }

}