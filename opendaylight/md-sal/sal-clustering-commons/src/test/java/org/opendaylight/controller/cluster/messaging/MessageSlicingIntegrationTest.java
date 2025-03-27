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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.opendaylight.controller.cluster.messaging.MessageSlicerTest.slice;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.testkit.TestProbe;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.raft.spi.FileBackedOutputStream;
import org.opendaylight.raft.spi.FileBackedOutputStreamFactory;
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
    private final BiConsumer<Object, ActorRef> mockAssembledMessageCallback = mock(BiConsumer.class);

    private final MessageAssembler assembler = MessageAssembler.builder()
            .assembledMessageCallback(mockAssembledMessageCallback).logContext("test")
            .fileBackedStreamFactory(FILE_BACKED_STREAM_FACTORY).build();

    @Before
    public void setup() {
        doNothing().when(mockOnFailureCallback).accept(any(Throwable.class));
        doNothing().when(mockAssembledMessageCallback).accept(any(Object.class), any(ActorRef.class));
    }

    @After
    public void tearDown() {
        assembler.close();
    }

    @AfterClass
    public static void staticTearDown() {
        TestKit.shutdownActorSystem(ACTOR_SYSTEM, true);
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

        testSlicing("testSlicingWithChunks", messageSliceSize, expTotalSlices, byteStream.toByteArray());

        // Now slice a message where the messageSliceSize doesn't divide evenly.

        byteStream.write(new byte[]{100, 101, 102});
        testSlicing("testSlicingWithChunks", messageSliceSize, expTotalSlices + 1, byteStream.toByteArray());

        LOG.info("testSlicingWithChunks ending");
    }

    @Test
    public void testSingleSlice() {
        LOG.info("testSingleSlice starting");

        // Slice a message where the serialized size is equal to the messageSliceSize. In this case it should
        // just send the original message.

        final BytesMessage message = new BytesMessage(new byte[]{1, 2, 3});
        try (MessageSlicer slicer = newMessageSlicer("testSingleSlice", SerializationUtils.serialize(message).length)) {
            final boolean wasSliced = slice(slicer, IDENTIFIER, message, sendToProbe.ref(), replyToProbe.ref(),
                    mockOnFailureCallback);
            assertFalse(wasSliced);

            final BytesMessage sentMessage = sendToProbe.expectMsgClass(BytesMessage.class);
            assertEquals("Sent message", message, sentMessage);
        }

        LOG.info("testSingleSlice ending");
    }

    @Test
    public void testSlicingWithRetry() {
        LOG.info("testSlicingWithRetry starting");

        final BytesMessage message = new BytesMessage(new byte[]{1, 2, 3});
        final int messageSliceSize = SerializationUtils.serialize(message).length / 2;
        try (MessageSlicer slicer = newMessageSlicer("testSlicingWithRetry", messageSliceSize)) {
            slice(slicer, IDENTIFIER, message, sendToProbe.ref(), replyToProbe.ref(), mockOnFailureCallback);

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

            assertAssembledMessage(message, replyToProbe.ref());
        }

        LOG.info("testSlicingWithRetry ending");
    }

    @Test
    public void testSlicingWithMaxRetriesReached() {
        LOG.info("testSlicingWithMaxRetriesReached starting");

        final BytesMessage message = new BytesMessage(new byte[]{1, 2, 3});
        final int messageSliceSize = SerializationUtils.serialize(message).length / 2;
        try (MessageSlicer slicer = newMessageSlicer("testSlicingWithMaxRetriesReached", messageSliceSize)) {
            slice(slicer, IDENTIFIER, message, sendToProbe.ref(), replyToProbe.ref(), mockOnFailureCallback);

            Identifier slicingId = null;
            for (int i = 0; i < MessageSlicer.DEFAULT_MAX_SLICING_TRIES; i++) {
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

            assertFalse("MessageSlicer did not remove state for " + slicingId, slicer.hasState(slicingId));
            assertFalse("MessageAssembler did not remove state for " + slicingId, assembler.hasState(slicingId));
        }

        LOG.info("testSlicingWithMaxRetriesReached ending");
    }

    @Test
    public void testSlicingWithFailure() {
        LOG.info("testSlicingWithFailure starting");

        final BytesMessage message = new BytesMessage(new byte[]{1, 2, 3});
        final int messageSliceSize = SerializationUtils.serialize(message).length / 2;
        try (MessageSlicer slicer = newMessageSlicer("testSlicingWithFailure", messageSliceSize)) {
            final boolean wasSliced = slice(slicer, IDENTIFIER, message, sendToProbe.ref(), replyToProbe.ref(),
                    mockOnFailureCallback);
            assertTrue(wasSliced);

            MessageSlice sliceMessage = sendToProbe.expectMsgClass(MessageSlice.class);

            MessageSliceException failure = new MessageSliceException("mock failure",
                    new IOException("mock IOException"));
            slicer.handleMessage(MessageSliceReply.failed(sliceMessage.getIdentifier(), failure, sendToProbe.ref()));

            assertFailureCallback(IOException.class);

            assertFalse("MessageSlicer did not remove state for " + sliceMessage.getIdentifier(),
                    slicer.hasState(sliceMessage.getIdentifier()));
        }

        LOG.info("testSlicingWithFailure ending");
    }

    @Test
    public void testSliceWithFileBackedOutputStream() throws IOException {
        LOG.info("testSliceWithFileBackedOutputStream starting");

        final BytesMessage message = new BytesMessage(new byte[]{1, 2, 3});
        FileBackedOutputStream fileBackedStream = FILE_BACKED_STREAM_FACTORY.newInstance();
        try (ObjectOutputStream out = new ObjectOutputStream(fileBackedStream)) {
            out.writeObject(message);
        }

        try (MessageSlicer slicer = newMessageSlicer("testSliceWithFileBackedOutputStream",
                SerializationUtils.serialize(message).length)) {
            slicer.slice(SliceOptions.builder().identifier(IDENTIFIER).fileBackedOutputStream(fileBackedStream)
                    .sendTo(ACTOR_SYSTEM.actorSelection(sendToProbe.ref().path())).replyTo(replyToProbe.ref())
                    .onFailureCallback(mockOnFailureCallback).build());

            final MessageSlice sliceMessage = sendToProbe.expectMsgClass(MessageSlice.class);
            assembler.handleMessage(sliceMessage, sendToProbe.ref());
            assertAssembledMessage(message, replyToProbe.ref());
        }

        LOG.info("testSliceWithFileBackedOutputStream ending");
    }

    @SuppressWarnings("unchecked")
    private void testSlicing(final String logContext, final int messageSliceSize, final int expTotalSlices,
            final byte[] messageData) {
        reset(mockAssembledMessageCallback);

        final BytesMessage message = new BytesMessage(messageData);

        try (MessageSlicer slicer = newMessageSlicer(logContext, messageSliceSize)) {
            final boolean wasSliced = slice(slicer, IDENTIFIER, message, sendToProbe.ref(), replyToProbe.ref(),
                    mockOnFailureCallback);
            assertTrue(wasSliced);

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

            assertAssembledMessage(message, replyToProbe.ref());

            assertFalse("MessageSlicer did not remove state for " + slicingId, slicer.hasState(slicingId));
            assertFalse("MessageAssembler did not remove state for " + slicingId, assembler.hasState(slicingId));
        }
    }

    private void assertFailureCallback(final Class<?> exceptionType) {
        ArgumentCaptor<Throwable> exceptionCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(mockOnFailureCallback).accept(exceptionCaptor.capture());
        assertEquals("Exception type", exceptionType, exceptionCaptor.getValue().getClass());
    }

    private void assertAssembledMessage(final BytesMessage message, final ActorRef sender) {
        assertAssembledMessage(mockAssembledMessageCallback, message, sender);
    }

    static void assertAssembledMessage(final BiConsumer<Object, ActorRef> mockAssembledMessageCallback,
            final BytesMessage message, final ActorRef sender) {
        ArgumentCaptor<Object> assembledMessageCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<ActorRef> senderActorRefCaptor = ArgumentCaptor.forClass(ActorRef.class);
        verify(mockAssembledMessageCallback).accept(assembledMessageCaptor.capture(), senderActorRefCaptor.capture());
        assertEquals("Assembled message", message, assembledMessageCaptor.getValue());
        assertEquals("Sender ActorRef", sender, senderActorRefCaptor.getValue());
    }

    static void assertSuccessfulMessageSliceReply(final MessageSliceReply reply, final Identifier identifier,
            final int sliceIndex) {
        assertEquals("Identifier", identifier, ((MessageSliceIdentifier)reply.getIdentifier())
                .getClientIdentifier());
        assertEquals("SliceIndex", sliceIndex, reply.getSliceIndex());
    }

    static void assertFailedMessageSliceReply(final MessageSliceReply reply, final Identifier identifier,
            final boolean isRetriable) {
        assertEquals("Identifier", identifier, ((MessageSliceIdentifier)reply.getIdentifier())
                .getClientIdentifier());
        assertEquals("Failure present", Boolean.TRUE, reply.getFailure().isPresent());
        assertEquals("isRetriable", isRetriable, reply.getFailure().orElseThrow().isRetriable());
    }

    static void assertMessageSlice(final MessageSlice sliceMessage, final Identifier identifier, final int sliceIndex,
            final int totalSlices, final int lastSliceHashCode, final ActorRef replyTo) {
        assertEquals("Identifier", identifier, ((MessageSliceIdentifier)sliceMessage.getIdentifier())
                .getClientIdentifier());
        assertEquals("SliceIndex", sliceIndex, sliceMessage.getSliceIndex());
        assertEquals("LastSliceHashCode", lastSliceHashCode, sliceMessage.getLastSliceHashCode());
        assertEquals("ReplyTo", replyTo, sliceMessage.getReplyTo());

        if (totalSlices != DONT_CARE) {
            assertEquals("TotalSlices", totalSlices, sliceMessage.getTotalSlices());
        }
    }

    private static MessageSlicer newMessageSlicer(final String logContext, final int messageSliceSize) {
        return MessageSlicer.builder().messageSliceSize(messageSliceSize).logContext(logContext)
                .fileBackedStreamFactory(FILE_BACKED_STREAM_FACTORY).build();
    }
}
