/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import akka.testkit.TestProbe;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.controller.cluster.io.FileBackedOutputStreamFactory;
import org.opendaylight.yangtools.concepts.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * End-to-end integration tests for message slicing.
 *
 * @author Thomas Pantelis
 */
public class MessageSlicingIntegrationTest {
    private static final Logger LOG = LoggerFactory.getLogger(MessageSlicingIntegrationTest.class);

    private static final ActorSystem ACTOR_SYSTEM = ActorSystem.create("test");
    private static final FileBackedOutputStreamFactory FILE_BACKED_STREAM_FACTORY =
            new FileBackedOutputStreamFactory(1000000000, "target");
    private static final Identifier IDENTIFIER = new StringIdentifier("stringId");
    private static final int DONT_CARE = -1;

    private final TestProbe sendToProbe = TestProbe.apply(ACTOR_SYSTEM);
    private final TestProbe replyToProbe = TestProbe.apply(ACTOR_SYSTEM);

    @SuppressWarnings("unchecked")
    private final Consumer<Throwable> mockOnFailureCallback = mock(Consumer.class);

    @SuppressWarnings("unchecked")
    private final Consumer<Object> mockAssembledMessageCallback = mock(Consumer.class);

    private final MessageAssembler assembler = new MessageAssembler(mockAssembledMessageCallback,
            FILE_BACKED_STREAM_FACTORY);

    @Before
    public void setup() {
        doNothing().when(mockOnFailureCallback).accept(any(Throwable.class));
        doNothing().when(mockAssembledMessageCallback).accept(any(Object.class));
    }

    @AfterClass
    public static void tearDown() {
        JavaTestKit.shutdownActorSystem(ACTOR_SYSTEM, Boolean.TRUE);
    }

    @Test
    public void testSlicingWithChunks() throws IOException {
        LOG.info("testSlicingWithChunks starting");

        // First slice a message where the messageSliceSize divides evenly into the serialized size.

        byte[] emptyMessageBytes = SerializationUtils.serialize(new BytesMessage(new byte[]{}));
        int messageSliceSize = 10;
        int expTotalSlices = emptyMessageBytes.length / messageSliceSize;
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        if (emptyMessageBytes.length % messageSliceSize > 0) {
            expTotalSlices++;
            int padding = messageSliceSize - emptyMessageBytes.length % messageSliceSize;
            byte value = 1;
            for (int i = 0; i < padding; i++, value++) {
                byteStream.write(value);
            }
        }

        testSlicing(messageSliceSize, expTotalSlices, byteStream.toByteArray());

        // Now slice a message where the messageSliceSize doesn't divide evenly.

        byteStream.write(new byte[]{100, 101, 102});
        testSlicing(messageSliceSize, expTotalSlices + 1, byteStream.toByteArray());

        LOG.info("testSlicingWithChunks ending");
    }

    @Test
    public void testSingleSlice() {
        LOG.info("testSingleSlice starting");

        // Slice a message where the serialized size is equal to the messageSliceSize. In this case it should
        // just send the original message.

        final BytesMessage message = new BytesMessage(new byte[]{1, 2, 3});
        final MessageSlicer slicer = new MessageSlicer(SerializationUtils.serialize(message).length,
                FILE_BACKED_STREAM_FACTORY);
        slicer.slice(IDENTIFIER, message, sendToProbe.ref(), replyToProbe.ref(), mockOnFailureCallback);

        final BytesMessage sentMessage = sendToProbe.expectMsgClass(BytesMessage.class);
        assertEquals("Sent message", message, sentMessage);

        LOG.info("testSingleSlice ending");
    }

    @Test
    public void testSlicingWithRetry() {
        LOG.info("testSlicingWithRetry starting");

        final BytesMessage message = new BytesMessage(new byte[]{1, 2, 3});
        final int messageSliceSize = SerializationUtils.serialize(message).length / 2;
        final MessageSlicer slicer = new MessageSlicer(messageSliceSize,
                FILE_BACKED_STREAM_FACTORY);
        slicer.slice(IDENTIFIER, message, sendToProbe.ref(), replyToProbe.ref(), mockOnFailureCallback);

        MessageSlice sliceMessage = sendToProbe.expectMsgClass(MessageSlice.class);
        assembler.handleMessage(sliceMessage, sendToProbe.ref());

        // Swallow the reply and send the MessageSlice again - it should return a failed reply.
        replyToProbe.expectMsgClass(MessageSliceReply.class);
        assembler.handleMessage(sliceMessage, sendToProbe.ref());

        final MessageSliceReply failedReply = replyToProbe.expectMsgClass(MessageSliceReply.class);
        assertFailedMessageSliceReply(failedReply, IDENTIFIER, true);

        // Send the failed reply - slicing should be retried from the beginning.

        slicer.handleMessage(failedReply);
        while (true) {
            sliceMessage = sendToProbe.expectMsgClass(MessageSlice.class);
            assembler.handleMessage(sliceMessage, sendToProbe.ref());

            final MessageSliceReply reply = replyToProbe.expectMsgClass(MessageSliceReply.class);
            assertSuccessfulMessageSliceReply(reply, IDENTIFIER, sliceMessage.getSliceIndex());
            slicer.handleMessage(reply);

            if (reply.getSliceIndex() == sliceMessage.getTotalSlices()) {
                break;
            }
        }

        assertAssembledMessage(message);

        LOG.info("testSlicingWithRetry ending");
    }

    @Test
    public void testSlicingWithMaxRetriesReached() {
        LOG.info("testSlicingWithMaxRetriesReached starting");

        final BytesMessage message = new BytesMessage(new byte[]{1, 2, 3});
        final int messageSliceSize = SerializationUtils.serialize(message).length / 2;
        final MessageSlicer slicer = new MessageSlicer(messageSliceSize,
                FILE_BACKED_STREAM_FACTORY);
        slicer.slice(IDENTIFIER, message, sendToProbe.ref(), replyToProbe.ref(), mockOnFailureCallback);

        Identifier slicingId = null;
        for (int i = 0; i < MessageSlicer.MAX_RETRIES; i++) {
            MessageSlice sliceMessage = sendToProbe.expectMsgClass(MessageSlice.class);
            slicingId = sliceMessage.getIdentifier();
            assertMessageSlice(sliceMessage, IDENTIFIER, 1, DONT_CARE, SlicedMessageState.INITIAL_SLICE_HASH_CODE,
                    replyToProbe.ref());
            assembler.handleMessage(sliceMessage, sendToProbe.ref());

            // Swallow the reply and send the MessageSlicer a reply with an invalid index.
            final MessageSliceReply reply = replyToProbe.expectMsgClass(MessageSliceReply.class);
            assertSuccessfulMessageSliceReply(reply, IDENTIFIER, sliceMessage.getSliceIndex());
            slicer.handleMessage(MessageSliceReply.success(reply.getIdentifier(), 100000, reply.getSendTo()));

            final AbortSlicing abortSlicing = sendToProbe.expectMsgClass(AbortSlicing.class);
            assertEquals("Identifier", slicingId, abortSlicing.getIdentifier());
            assembler.handleMessage(abortSlicing, sendToProbe.ref());
        }

        slicer.handleMessage(MessageSliceReply.success(slicingId, 100000, sendToProbe.ref()));

        assertFailureCallback(RuntimeException.class);

        assertFalse("MessageSlicer did not remove state for ", slicer.hasState(slicingId));
        assertFalse("MessageAssembler did not remove state for ", assembler.hasState(slicingId));

        LOG.info("testSlicingWithMaxRetriesReached ending");
    }

    @Test
    public void testSlicingWithFailure() {
        LOG.info("testSlicingWithFailure starting");

        final BytesMessage message = new BytesMessage(new byte[]{1, 2, 3});
        final int messageSliceSize = SerializationUtils.serialize(message).length / 2;
        final MessageSlicer slicer = new MessageSlicer(messageSliceSize,
                FILE_BACKED_STREAM_FACTORY);
        slicer.slice(IDENTIFIER, message, sendToProbe.ref(), replyToProbe.ref(), mockOnFailureCallback);

        MessageSlice sliceMessage = sendToProbe.expectMsgClass(MessageSlice.class);

        MessageSliceException failure = new MessageSliceException("mock failure", new IOException("mock IOException"));
        slicer.handleMessage(MessageSliceReply.failed(sliceMessage.getIdentifier(), failure, sendToProbe.ref()));

        assertFailureCallback(IOException.class);

        assertFalse("MessageSlicer did not remove state for ", slicer.hasState(sliceMessage.getIdentifier()));

        LOG.info("testSlicingWithFailure ending");
    }

    @SuppressWarnings("unchecked")
    private void testSlicing(int messageSliceSize, int expTotalSlices, byte[] messageData) {
        reset(mockAssembledMessageCallback);

        final BytesMessage message = new BytesMessage(messageData);

        final MessageSlicer slicer = new MessageSlicer(messageSliceSize , FILE_BACKED_STREAM_FACTORY);
        slicer.slice(IDENTIFIER, message, sendToProbe.ref(), replyToProbe.ref(), mockOnFailureCallback);

        Identifier slicingId = null;
        int expLastSliceHashCode = SlicedMessageState.INITIAL_SLICE_HASH_CODE;
        for (int sliceIndex = 1; sliceIndex <= expTotalSlices; sliceIndex++) {
            final MessageSlice sliceMessage = sendToProbe.expectMsgClass(MessageSlice.class);
            slicingId = sliceMessage.getIdentifier();
            assertMessageSlice(sliceMessage, IDENTIFIER, sliceIndex, expTotalSlices, expLastSliceHashCode,
                    replyToProbe.ref());

            assembler.handleMessage(sliceMessage, sendToProbe.ref());

            final MessageSliceReply reply = replyToProbe.expectMsgClass(MessageSliceReply.class);
            assertSuccessfulMessageSliceReply(reply, IDENTIFIER, sliceIndex);

            expLastSliceHashCode = Arrays.hashCode(sliceMessage.getData());

            slicer.handleMessage(reply);
        }

        assertAssembledMessage(message);

        assertFalse("MessageSlicer did not remove state for ", slicer.hasState(slicingId));
        assertFalse("MessageAssembler did not remove state for ", assembler.hasState(slicingId));
    }

    private void assertAssembledMessage(final BytesMessage message) {
        ArgumentCaptor<Object> assembledMessageCaptor = ArgumentCaptor.forClass(Object.class);
        verify(mockAssembledMessageCallback).accept(assembledMessageCaptor.capture());
        assertEquals("Assembled message", message, assembledMessageCaptor.getValue());
    }

    private void assertFailureCallback(final Class<?> exceptionType) {
        ArgumentCaptor<Throwable> exceptionCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(mockOnFailureCallback).accept(exceptionCaptor.capture());
        assertEquals("Exception type", exceptionType, exceptionCaptor.getValue().getClass());
    }

    private void assertSuccessfulMessageSliceReply(MessageSliceReply reply, Identifier identifier, int sliceIndex) {
        assertEquals("Identifier", identifier, ((MessageSliceIdentifier)reply.getIdentifier())
                .getClientIdentifier());
        assertEquals("SliceIndex", sliceIndex, reply.getSliceIndex());
    }

    private void assertFailedMessageSliceReply(MessageSliceReply reply, Identifier identifier, boolean isRetriable) {
        assertEquals("Identifier", identifier, ((MessageSliceIdentifier)reply.getIdentifier())
                .getClientIdentifier());
        assertEquals("Failure present", Boolean.TRUE, reply.getFailure().isPresent());
        assertEquals("isRetriable", isRetriable, reply.getFailure().get().isRetriable());
    }

    private void assertMessageSlice(MessageSlice sliceMessage, Identifier identifier, int sliceIndex, int totalSlices,
            int lastSliceHashCode, ActorRef replyTo) {
        assertEquals("Identifier", identifier, ((MessageSliceIdentifier)sliceMessage.getIdentifier())
                .getClientIdentifier());
        assertEquals("SliceIndex", sliceIndex, sliceMessage.getSliceIndex());
        assertEquals("LastSliceHashCode", lastSliceHashCode, sliceMessage.getLastSliceHashCode());
        assertEquals("ReplyTo", replyTo, sliceMessage.getReplyTo());

        if (totalSlices != DONT_CARE) {
            assertEquals("TotalSlices", totalSlices, sliceMessage.getTotalSlices());
        }
    }
}
