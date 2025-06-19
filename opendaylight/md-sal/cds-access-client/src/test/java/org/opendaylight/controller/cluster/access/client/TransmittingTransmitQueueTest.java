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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.opendaylight.controller.cluster.access.client.ConnectionEntryMatcher.entryWithRequest;

import com.google.common.base.Ticker;
import com.google.common.collect.Collections2;
import com.google.common.testing.FakeTicker;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequestBuilder;
import org.opendaylight.controller.cluster.access.commands.TransactionPurgeRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionPurgeResponse;
import org.opendaylight.controller.cluster.access.concepts.FailureEnvelope;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.SuccessEnvelope;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.messaging.MessageSlicer;
import org.opendaylight.controller.cluster.messaging.SliceOptions;

@ExtendWith(MockitoExtension.class)
class TransmittingTransmitQueueTest extends AbstractTransmitQueueTest<TransmitQueue.Transmitting> {
    @Mock
    private MessageSlicer mockMessageSlicer;

    private BackendInfo backendInfo;

    private static long now() {
        return Ticker.systemTicker().read();
    }

    @Override
    int getMaxInFlightMessages() {
        return backendInfo.getMaxMessages();
    }

    @Override
    TransmitQueue.Transmitting createQueue() {
        backendInfo = new BackendInfo(probe.ref(), "test", 0L, ABIVersion.current(), 3);
        return new TransmitQueue.Transmitting(new TransmitQueue.Halted(0), 0, backendInfo, now(), mockMessageSlicer);
    }

    @Test
    void testComplete() {
        final long sequence1 = 0L;
        final long sequence2 = 1L;
        final var request1 = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, sequence1, probe.ref());
        final var transactionIdentifier2 = new TransactionIdentifier(HISTORY, 1L);
        final var request2 = new TransactionPurgeRequest(transactionIdentifier2, sequence2, probe.ref());
        final var callback1 = createConsumerMock();
        final var callback2 = createConsumerMock();
        final long now1 = now();
        final long now2 = now();
        //enqueue 2 entries
        queue.enqueueOrForward(new ConnectionEntry(request1, callback1, now1), now1);
        queue.enqueueOrForward(new ConnectionEntry(request2, callback2, now2), now2);
        final var success1 = new TransactionPurgeResponse(TRANSACTION_IDENTIFIER, sequence1);
        final var success2 = new TransactionPurgeResponse(transactionIdentifier2, sequence2);
        //complete entries in different order
        final var transmittedEntry2 = queue.complete(new SuccessEnvelope(success2, 0L, sequence2, 1L), now2);
        assertNotNull(transmittedEntry2);
        final var transmittedEntry1 = queue.complete(new SuccessEnvelope(success1, 0L, sequence1, 1L), now1);
        assertNotNull(transmittedEntry1);

        //check first entry
        assertEquals(transmittedEntry1.getRequest(), request1);
        assertEquals(transmittedEntry1.getTxSequence(), sequence1);
        assertEquals(transmittedEntry1.getCallback(), callback1);
        //check second entry
        assertEquals(transmittedEntry2.getRequest(), request2);
        assertEquals(transmittedEntry2.getTxSequence(), sequence2);
        assertEquals(transmittedEntry2.getCallback(), callback2);
    }

    @Test
    void testEnqueueCanTransmit() {
        final var request = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 0L, probe.ref());
        final var callback = createConsumerMock();
        final long now = now();
        queue.enqueueOrForward(new ConnectionEntry(request, callback, now), now);
        final var requestEnvelope = probe.expectMsgClass(RequestEnvelope.class);
        assertEquals(request, requestEnvelope.getMessage());
    }

    @Test
    void testEnqueueBackendFull() {
        final var request = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 0L, probe.ref());
        final var callback = createConsumerMock();
        final long now = now();
        final int sentMessages = getMaxInFlightMessages() + 1;
        for (int i = 0; i < sentMessages; i++) {
            queue.enqueueOrForward(new ConnectionEntry(request, callback, now), now);
        }
        for (int i = 0; i < getMaxInFlightMessages(); i++) {
            probe.expectMsgClass(RequestEnvelope.class);
        }
        probe.expectNoMessage();
        final var entries = queue.drain();
        assertEquals(sentMessages, entries.size());
        assertThat(entries, everyItem(entryWithRequest(request)));
    }

    @Test
    @Override
    void testCanTransmitCount() {
        assertTrue(queue.canTransmitCount(getMaxInFlightMessages() - 1) > 0);
        assertFalse(queue.canTransmitCount(getMaxInFlightMessages()) > 0);
    }

    @Test
    @Override
    void testTransmit() {
        final var request1 = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 0L, probe.ref());
        final var callback = createConsumerMock();
        final long now = now();
        final var entry = new ConnectionEntry(request1, callback, now);

        var transmitted = queue.transmit(entry, now);
        assertNotNull(transmitted);
        assertEquals(request1, transmitted.getRequest());
        assertEquals(callback, transmitted.getCallback());

        final var requestEnvelope = probe.expectMsgClass(RequestEnvelope.class);
        assertEquals(request1, requestEnvelope.getMessage());

        final var request2 = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 1L, probe.ref());
        transmitted = queue.transmit(new ConnectionEntry(request2, callback, now), now);
        assertNotNull(transmitted);
        assertEquals(request2, transmitted.getRequest());
        assertEquals(callback, transmitted.getCallback());
    }

    @Test
    void testSetForwarder() {
        final var ticker = new FakeTicker().setAutoIncrementStep(1, TimeUnit.MICROSECONDS);
        final var request = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 0L, probe.ref());
        final var callback = createConsumerMock();
        final var entry = new ConnectionEntry(request, callback, ticker.read());
        final var forwarder = mock(ReconnectForwarder.class);
        queue.setForwarder(forwarder, ticker.read());
        final long secondEnqueueNow = ticker.read();
        queue.enqueueOrForward(entry, secondEnqueueNow);
        verify(forwarder).forwardEntry(entry, secondEnqueueNow);
    }

    @Test
    void testCompleteOrdering() {
        final var req0 = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 0L, probe.ref());
        final var req1 = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 1L, probe.ref());
        final var req2 = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 2L, probe.ref());
        final var req3 = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 3L, probe.ref());
        final var req4 = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 4L, probe.ref());
        final var req5 = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 5L, probe.ref());
        final var req6 = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 6L, probe.ref());
        final var callback = createConsumerMock();

        // Fill the queue up to capacity + 1
        queue.enqueueOrForward(new ConnectionEntry(req0, callback, 0), 0);
        queue.enqueueOrForward(new ConnectionEntry(req1, callback, 0), 0);
        queue.enqueueOrForward(new ConnectionEntry(req2, callback, 0), 0);
        queue.enqueueOrForward(new ConnectionEntry(req3, callback, 0), 0);
        assertEqualRequests(queue.getInflight(), req0, req1, req2);
        assertEqualRequests(queue.getPending(), req3);

        // Now complete req0, which should transmit req3
        queue.complete(new FailureEnvelope(req0.toRequestFailure(mock(RequestException.class)), 0, 0, 0), 0);
        assertEqualRequests(queue.getInflight(), req1, req2, req3);
        assertEqualRequests(queue.getPending());

        // Now complete req1, which should leave an empty slot
        queue.complete(new FailureEnvelope(req1.toRequestFailure(mock(RequestException.class)), 0, 1, 0), 0);
        assertEqualRequests(queue.getInflight(), req2, req3);
        assertEqualRequests(queue.getPending());

        // Enqueue req4, which should be immediately transmitted
        queue.enqueueOrForward(new ConnectionEntry(req4, callback, 0), 0);
        assertEqualRequests(queue.getInflight(), req2, req3, req4);
        assertEqualRequests(queue.getPending());

        // Enqueue req5, which should move to pending
        queue.enqueueOrForward(new ConnectionEntry(req5, callback, 0), 0);
        assertEqualRequests(queue.getInflight(), req2, req3, req4);
        assertEqualRequests(queue.getPending(), req5);

        // Remove req4, creating an inconsistency...
        queue.getInflight().removeLast();
        assertEqualRequests(queue.getInflight(), req2, req3);
        assertEqualRequests(queue.getPending(), req5);

        // ... and enqueue req6, which should cause req5 to be transmitted
        queue.enqueueOrForward(new ConnectionEntry(req6, callback, 0), 0);
        assertEqualRequests(queue.getInflight(), req2, req3, req5);
        assertEqualRequests(queue.getPending(), req6);
    }

    @Test
    void testRequestSlicingOnTransmit() {
        doReturn(true).when(mockMessageSlicer).slice(any());

        final var request = new ModifyTransactionRequestBuilder(TRANSACTION_IDENTIFIER, probe.ref())
            .setSequence(0L)
            .build();

        final long now = now();
        final var mockConsumer = createConsumerMock();
        assertNotNull(queue.transmit(new ConnectionEntry(request, mockConsumer, now), now));

        final var sliceOptions = ArgumentCaptor.forClass(SliceOptions.class);
        verify(mockMessageSlicer).slice(sliceOptions.capture());
        final var requestEnvelope = assertInstanceOf(RequestEnvelope.class, sliceOptions.getValue().getMessage());
        assertEquals(request, requestEnvelope.getMessage());

        final var request2 = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 1L, probe.ref());
        assertNull(queue.transmit(new ConnectionEntry(request2, mockConsumer, now), now));
    }

    @Test
    void testSlicingFailureOnTransmit() {
        doAnswer(invocation -> {
            invocation.<SliceOptions>getArgument(0).getOnFailureCallback().accept(new Exception("mock"));
            return Boolean.FALSE;
        }).when(mockMessageSlicer).slice(any());

        final var reqBuilder = new ModifyTransactionRequestBuilder(TRANSACTION_IDENTIFIER, probe.ref())
            .setSequence(0L);

        final long now = now();
        assertNotNull(queue.transmit(new ConnectionEntry(reqBuilder.build(), createConsumerMock(), now), now));

        verify(mockMessageSlicer).slice(any());

        probe.expectMsgClass(FailureEnvelope.class);
    }

    @Test
    void testSlicedRequestOnComplete() {
        doReturn(true).when(mockMessageSlicer).slice(any());

        final var request = new ModifyTransactionRequestBuilder(TRANSACTION_IDENTIFIER, probe.ref())
            .setSequence(0L)
            .build();

        final long now = now();
        final var mockConsumer = createConsumerMock();
        queue.enqueueOrForward(new ConnectionEntry(request, mockConsumer, now), now);

        final var sliceOptions = ArgumentCaptor.forClass(SliceOptions.class);
        verify(mockMessageSlicer).slice(sliceOptions.capture());
        final var requestEnvelope = assertInstanceOf(RequestEnvelope.class, sliceOptions.getValue().getMessage());

        final var request2 = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 1L, probe.ref());
        queue.enqueueOrForward(new ConnectionEntry(request2, mockConsumer, now), now);
        verifyNoMoreInteractions(mockMessageSlicer);
        probe.expectNoMessage();

        queue.complete(new FailureEnvelope(request.toRequestFailure(mock(RequestException.class)),
                requestEnvelope.getSessionId(), requestEnvelope.getTxSequence(), 0), 0);

        assertEquals(request2, probe.expectMsgClass(RequestEnvelope.class).getMessage());

        final var request3 = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 3L, probe.ref());
        queue.enqueueOrForward(new ConnectionEntry(request3, mockConsumer, now), now);

        assertEquals(request3, probe.expectMsgClass(RequestEnvelope.class).getMessage());
    }

    private static void assertEqualRequests(final Collection<? extends ConnectionEntry> queue,
            final Request<?, ?>... requests) {
        assertEquals(List.of(requests), List.copyOf(Collections2.transform(queue, ConnectionEntry::getRequest)));
    }
}
