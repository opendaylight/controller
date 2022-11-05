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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.opendaylight.controller.cluster.access.client.ConnectionEntryMatcher.entryWithRequest;

import com.google.common.base.Ticker;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.testing.FakeTicker;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequestBuilder;
import org.opendaylight.controller.cluster.access.commands.TransactionPurgeRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionPurgeResponse;
import org.opendaylight.controller.cluster.access.concepts.FailureEnvelope;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestSuccess;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.SuccessEnvelope;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.messaging.MessageSlicer;
import org.opendaylight.controller.cluster.messaging.SliceOptions;

public class TransmittingTransmitQueueTest extends AbstractTransmitQueueTest<TransmitQueue.Transmitting> {

    private BackendInfo backendInfo;
    private final MessageSlicer mockMessageSlicer = mock(MessageSlicer.class);

    private static long now() {
        return Ticker.systemTicker().read();
    }

    @Override
    protected int getMaxInFlightMessages() {
        return backendInfo.getMaxMessages();
    }

    @Override
    protected TransmitQueue.Transmitting createQueue() {
        doReturn(false).when(mockMessageSlicer).slice(any());
        backendInfo = new BackendInfo(probe.ref(), "test", 0L, ABIVersion.current(), 3);
        return new TransmitQueue.Transmitting(new TransmitQueue.Halted(0), 0, backendInfo, now(), mockMessageSlicer);
    }

    @Test
    public void testComplete() {
        final long sequence1 = 0L;
        final long sequence2 = 1L;
        final Request<?, ?> request1 = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, sequence1, probe.ref());
        final TransactionIdentifier transactionIdentifier2 = new TransactionIdentifier(HISTORY, 1L);
        final Request<?, ?> request2 = new TransactionPurgeRequest(transactionIdentifier2, sequence2, probe.ref());
        final Consumer<Response<?, ?>> callback1 = createConsumerMock();
        final Consumer<Response<?, ?>> callback2 = createConsumerMock();
        final long now1 = now();
        final long now2 = now();
        //enqueue 2 entries
        queue.enqueueOrForward(new ConnectionEntry(request1, callback1, now1), now1);
        queue.enqueueOrForward(new ConnectionEntry(request2, callback2, now2), now2);
        final RequestSuccess<?, ?> success1 = new TransactionPurgeResponse(TRANSACTION_IDENTIFIER, sequence1);
        final RequestSuccess<?, ?> success2 = new TransactionPurgeResponse(transactionIdentifier2, sequence2);
        //complete entries in different order
        final Optional<TransmittedConnectionEntry> completed2 =
                queue.complete(new SuccessEnvelope(success2, 0L, sequence2, 1L), now2);
        final Optional<TransmittedConnectionEntry> completed1 =
                queue.complete(new SuccessEnvelope(success1, 0L, sequence1, 1L), now1);
        //check first entry
        final TransmittedConnectionEntry transmittedEntry1 = completed1.orElseThrow(AssertionError::new);
        assertEquals(transmittedEntry1.getRequest(), request1);
        assertEquals(transmittedEntry1.getTxSequence(), sequence1);
        assertEquals(transmittedEntry1.getCallback(), callback1);
        //check second entry
        final TransmittedConnectionEntry transmittedEntry2 = completed2.orElseThrow(AssertionError::new);
        assertEquals(transmittedEntry2.getRequest(), request2);
        assertEquals(transmittedEntry2.getTxSequence(), sequence2);
        assertEquals(transmittedEntry2.getCallback(), callback2);
    }

    @Test
    public void testEnqueueCanTransmit() {
        final Request<?, ?> request = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 0L, probe.ref());
        final Consumer<Response<?, ?>> callback = createConsumerMock();
        final long now = now();
        queue.enqueueOrForward(new ConnectionEntry(request, callback, now), now);
        final RequestEnvelope requestEnvelope = probe.expectMsgClass(RequestEnvelope.class);
        assertEquals(request, requestEnvelope.getMessage());
    }

    @Test
    public void testEnqueueBackendFull() {
        final Request<?, ?> request = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 0L, probe.ref());
        final Consumer<Response<?, ?>> callback = createConsumerMock();
        final long now = now();
        final int sentMessages = getMaxInFlightMessages() + 1;
        for (int i = 0; i < sentMessages; i++) {
            queue.enqueueOrForward(new ConnectionEntry(request, callback, now), now);
        }
        for (int i = 0; i < getMaxInFlightMessages(); i++) {
            probe.expectMsgClass(RequestEnvelope.class);
        }
        probe.expectNoMessage();
        final Collection<ConnectionEntry> entries = queue.drain();
        assertEquals(sentMessages, entries.size());
        assertThat(entries, everyItem(entryWithRequest(request)));
    }

    @Test
    @Override
    public void testCanTransmitCount() {
        assertTrue(queue.canTransmitCount(getMaxInFlightMessages() - 1) > 0);
        assertFalse(queue.canTransmitCount(getMaxInFlightMessages()) > 0);
    }

    @Test
    @Override
    public void testTransmit() {
        final Request<?, ?> request = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 0L, probe.ref());
        final Consumer<Response<?, ?>> callback = createConsumerMock();
        final long now = now();
        final ConnectionEntry entry = new ConnectionEntry(request, callback, now);

        Optional<TransmittedConnectionEntry> transmitted = queue.transmit(entry, now);
        assertTrue(transmitted.isPresent());
        assertEquals(request, transmitted.get().getRequest());
        assertEquals(callback, transmitted.get().getCallback());

        final RequestEnvelope requestEnvelope = probe.expectMsgClass(RequestEnvelope.class);
        assertEquals(request, requestEnvelope.getMessage());

        transmitted = queue.transmit(new ConnectionEntry(new TransactionPurgeRequest(
                TRANSACTION_IDENTIFIER, 1L, probe.ref()), callback, now), now);
        assertTrue(transmitted.isPresent());
    }

    @Test
    public void testSetForwarder() {
        final FakeTicker ticker = new FakeTicker();
        ticker.setAutoIncrementStep(1, TimeUnit.MICROSECONDS);
        final Request<?, ?> request = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 0L, probe.ref());
        final Consumer<Response<?, ?>> callback = createConsumerMock();
        final ConnectionEntry entry = new ConnectionEntry(request, callback, ticker.read());
        final ReconnectForwarder forwarder = mock(ReconnectForwarder.class);
        queue.setForwarder(forwarder, ticker.read());
        final long secondEnqueueNow = ticker.read();
        queue.enqueueOrForward(entry, secondEnqueueNow);
        verify(forwarder).forwardEntry(entry, secondEnqueueNow);
    }

    @Test
    public void testCompleteOrdering() {
        final Request<?, ?> req0 = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 0L, probe.ref());
        final Request<?, ?> req1 = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 1L, probe.ref());
        final Request<?, ?> req2 = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 2L, probe.ref());
        final Request<?, ?> req3 = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 3L, probe.ref());
        final Request<?, ?> req4 = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 4L, probe.ref());
        final Request<?, ?> req5 = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 5L, probe.ref());
        final Request<?, ?> req6 = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 6L, probe.ref());
        final Consumer<Response<?, ?>> callback = createConsumerMock();

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
    public void testRequestSlicingOnTransmit() {
        doReturn(true).when(mockMessageSlicer).slice(any());

        ModifyTransactionRequestBuilder reqBuilder = new ModifyTransactionRequestBuilder(
                TRANSACTION_IDENTIFIER, probe.ref());
        reqBuilder.setSequence(0L);
        final Request<?, ?> request = reqBuilder.build();

        final long now = now();
        final Consumer<Response<?, ?>> mockConsumer = createConsumerMock();
        Optional<TransmittedConnectionEntry> transmitted =
                queue.transmit(new ConnectionEntry(request, mockConsumer, now), now);
        assertTrue(transmitted.isPresent());

        ArgumentCaptor<SliceOptions> sliceOptions = ArgumentCaptor.forClass(SliceOptions.class);
        verify(mockMessageSlicer).slice(sliceOptions.capture());
        assertTrue(sliceOptions.getValue().getMessage() instanceof RequestEnvelope);
        RequestEnvelope requestEnvelope = (RequestEnvelope) sliceOptions.getValue().getMessage();
        assertEquals(request, requestEnvelope.getMessage());

        final Request<?, ?> request2 = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 1L, probe.ref());
        transmitted = queue.transmit(new ConnectionEntry(request2, mockConsumer, now), now);
        assertFalse(transmitted.isPresent());
    }

    @Test
    public void testSlicingFailureOnTransmit() {
        doAnswer(invocation -> {
            invocation.<SliceOptions>getArgument(0).getOnFailureCallback().accept(new Exception("mock"));
            return Boolean.FALSE;
        }).when(mockMessageSlicer).slice(any());

        ModifyTransactionRequestBuilder reqBuilder = new ModifyTransactionRequestBuilder(
                TRANSACTION_IDENTIFIER, probe.ref());
        reqBuilder.setSequence(0L);

        final long now = now();
        Optional<TransmittedConnectionEntry> transmitted =
                queue.transmit(new ConnectionEntry(reqBuilder.build(), createConsumerMock(), now), now);
        assertTrue(transmitted.isPresent());

        verify(mockMessageSlicer).slice(any());

        probe.expectMsgClass(FailureEnvelope.class);
    }

    @Test
    public void testSlicedRequestOnComplete() {
        doReturn(true).when(mockMessageSlicer).slice(any());

        ModifyTransactionRequestBuilder reqBuilder = new ModifyTransactionRequestBuilder(
                TRANSACTION_IDENTIFIER, probe.ref());
        reqBuilder.setSequence(0L);
        final Request<?, ?> request = reqBuilder.build();

        final long now = now();
        final Consumer<Response<?, ?>> mockConsumer = createConsumerMock();
        queue.enqueueOrForward(new ConnectionEntry(request, mockConsumer, now), now);

        ArgumentCaptor<SliceOptions> sliceOptions = ArgumentCaptor.forClass(SliceOptions.class);
        verify(mockMessageSlicer).slice(sliceOptions.capture());
        assertTrue(sliceOptions.getValue().getMessage() instanceof RequestEnvelope);

        final Request<?, ?> request2 = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 1L, probe.ref());
        queue.enqueueOrForward(new ConnectionEntry(request2, mockConsumer, now), now);
        verifyNoMoreInteractions(mockMessageSlicer);
        probe.expectNoMessage();

        RequestEnvelope requestEnvelope = (RequestEnvelope) sliceOptions.getValue().getMessage();
        queue.complete(new FailureEnvelope(request.toRequestFailure(mock(RequestException.class)),
                requestEnvelope.getSessionId(), requestEnvelope.getTxSequence(), 0), 0);

        requestEnvelope = probe.expectMsgClass(RequestEnvelope.class);
        assertEquals(request2, requestEnvelope.getMessage());

        final Request<?, ?> request3 = new TransactionPurgeRequest(TRANSACTION_IDENTIFIER, 3L, probe.ref());
        queue.enqueueOrForward(new ConnectionEntry(request3, mockConsumer, now), now);

        requestEnvelope = probe.expectMsgClass(RequestEnvelope.class);
        assertEquals(request3, requestEnvelope.getMessage());
    }

    private static void assertEqualRequests(final Collection<? extends ConnectionEntry> queue,
            final Request<?, ?>... requests) {
        final List<Request<?, ?>> queued = ImmutableList.copyOf(Collections2.transform(queue,
            ConnectionEntry::getRequest));
        assertEquals(Arrays.asList(requests), queued);
    }
}
