/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.opendaylight.controller.cluster.messaging.MessageSlicerTest.slice;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.testkit.TestProbe;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.raft.spi.FileBackedOutputStreamFactory;
import org.opendaylight.yangtools.concepts.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * End-to-end integration tests for message slicing.
 *
 * @author Thomas Pantelis
 */
@ExtendWith(MockitoExtension.class)
class MessageSlicingIntegrationTest {
    private static final Logger LOG = LoggerFactory.getLogger(MessageSlicingIntegrationTest.class);

    private static final ActorSystem ACTOR_SYSTEM = ActorSystem.create("test");
    private static final Identifier IDENTIFIER = new StringIdentifier("stringId");
    private static final int DONT_CARE = -1;

    private final TestProbe sendToProbe = TestProbe.apply(ACTOR_SYSTEM);
    private final TestProbe replyToProbe = TestProbe.apply(ACTOR_SYSTEM);

    @TempDir
    private Path tempDir;
    @Mock
    private Consumer<Throwable> mockOnFailureCallback;
    @Mock
    private BiConsumer<Object, ActorRef> mockAssembledMessageCallback;

    private FileBackedOutputStreamFactory streamFactory;
    private MessageAssembler assembler;

    @BeforeEach
    void beforeEach() {
        doNothing().when(mockOnFailureCallback).accept(any(Throwable.class));
        doNothing().when(mockAssembledMessageCallback).accept(any(Object.class), any(ActorRef.class));

        streamFactory = new FileBackedOutputStreamFactory(1000000000, tempDir);
        assembler = MessageAssembler.builder()
            .assembledMessageCallback(mockAssembledMessageCallback)
            .fileBackedStreamFactory(streamFactory)
            .logContext("test")
            .build();
    }

    @AfterEach
    void tearDown() {
        assembler.close();
    }

    @AfterAll
    public static void staticTearDown() {
        TestKit.shutdownActorSystem(ACTOR_SYSTEM, true);
    }

    @Test
    void testSlicingWithChunks() throws Exception {
        LOG.info("testSlicingWithChunks starting");

        // First slice a message where the messageSliceSize divides evenly into the serialized size.

        byte[] emptyMessageBytes = SerializationUtils.serialize(new BytesMessage(new byte[]{}));
        int messageSliceSize = 10;
        int expTotalSlices = emptyMessageBytes.length / messageSliceSize;
        final var byteStream = new ByteArrayOutputStream();
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

        byteStream.write(new byte[] { 100, 101, 102 });
        testSlicing("testSlicingWithChunks", messageSliceSize, expTotalSlices + 1, byteStream.toByteArray());

        LOG.info("testSlicingWithChunks ending");
    }

    @Test
    void testSingleSlice() {
        LOG.info("testSingleSlice starting");

        // Slice a message where the serialized size is equal to the messageSliceSize. In this case it should
        // just send the original message.

        final var message = new BytesMessage(new byte[] { 1, 2, 3 });
        try (var slicer = newMessageSlicer("testSingleSlice", SerializationUtils.serialize(message).length)) {
            final boolean wasSliced = slice(slicer, IDENTIFIER, message, sendToProbe.ref(), replyToProbe.ref(),
                    mockOnFailureCallback);
            assertFalse(wasSliced);

            assertEquals(message, sendToProbe.expectMsgClass(BytesMessage.class));
        }

        LOG.info("testSingleSlice ending");
    }

    @Test
    void testSlicingWithRetry() {
        LOG.info("testSlicingWithRetry starting");

        final var message = new BytesMessage(new byte[] { 1, 2, 3 });
        final int messageSliceSize = SerializationUtils.serialize(message).length / 2;
        try (var slicer = newMessageSlicer("testSlicingWithRetry", messageSliceSize)) {
            slice(slicer, IDENTIFIER, message, sendToProbe.ref(), replyToProbe.ref(), mockOnFailureCallback);

            var sliceMessage = sendToProbe.expectMsgClass(MessageSlice.class);
            assembler.handleMessage(sliceMessage, sendToProbe.ref());

            // Swallow the reply and send the MessageSlice again - it should return a failed reply.
            replyToProbe.expectMsgClass(MessageSliceReply.class);
            assembler.handleMessage(sliceMessage, sendToProbe.ref());

            final var failedReply = replyToProbe.expectMsgClass(MessageSliceReply.class);
            assertFailedMessageSliceReply(failedReply, IDENTIFIER, true);

            // Send the failed reply - slicing should be retried from the beginning.

            slicer.handleMessage(failedReply);
            while (true) {
                sliceMessage = sendToProbe.expectMsgClass(MessageSlice.class);
                assembler.handleMessage(sliceMessage, sendToProbe.ref());

                final var reply = replyToProbe.expectMsgClass(MessageSliceReply.class);
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
    void testSlicingWithMaxRetriesReached() {
        LOG.info("testSlicingWithMaxRetriesReached starting");

        final var message = new BytesMessage(new byte[] { 1, 2, 3 });
        final int messageSliceSize = SerializationUtils.serialize(message).length / 2;
        try (var slicer = newMessageSlicer("testSlicingWithMaxRetriesReached", messageSliceSize)) {
            slice(slicer, IDENTIFIER, message, sendToProbe.ref(), replyToProbe.ref(), mockOnFailureCallback);

            Identifier slicingId = null;
            for (int i = 0; i < MessageSlicer.DEFAULT_MAX_SLICING_TRIES; i++) {
                var sliceMessage = sendToProbe.expectMsgClass(MessageSlice.class);
                slicingId = sliceMessage.getIdentifier();
                assertMessageSlice(sliceMessage, IDENTIFIER, 1, DONT_CARE, SlicedMessageState.INITIAL_SLICE_HASH_CODE,
                        replyToProbe.ref());
                assembler.handleMessage(sliceMessage, sendToProbe.ref());

                // Swallow the reply and send the MessageSlicer a reply with an invalid index.
                final var reply = replyToProbe.expectMsgClass(MessageSliceReply.class);
                assertSuccessfulMessageSliceReply(reply, IDENTIFIER, sliceMessage.getSliceIndex());
                slicer.handleMessage(MessageSliceReply.success(reply.getIdentifier(), 100000, reply.getSendTo()));

                final var abortSlicing = sendToProbe.expectMsgClass(AbortSlicing.class);
                assertEquals(slicingId, abortSlicing.getIdentifier());
                assembler.handleMessage(abortSlicing, sendToProbe.ref());
            }

            slicer.handleMessage(MessageSliceReply.success(slicingId, 100000, sendToProbe.ref()));

            assertFailureCallback(RuntimeException.class);

            assertFalse(slicer.hasState(slicingId), "MessageSlicer did not remove state for " + slicingId);
            assertFalse(assembler.hasState(slicingId), "MessageAssembler did not remove state for " + slicingId);
        }

        LOG.info("testSlicingWithMaxRetriesReached ending");
    }

    @Test
    void testSlicingWithFailure() {
        LOG.info("testSlicingWithFailure starting");

        final var message = new BytesMessage(new byte[]{1, 2, 3});
        final int messageSliceSize = SerializationUtils.serialize(message).length / 2;
        try (var slicer = newMessageSlicer("testSlicingWithFailure", messageSliceSize)) {
            final boolean wasSliced = slice(slicer, IDENTIFIER, message, sendToProbe.ref(), replyToProbe.ref(),
                    mockOnFailureCallback);
            assertTrue(wasSliced);

            var sliceMessage = sendToProbe.expectMsgClass(MessageSlice.class);

            var failure = new MessageSliceException("mock failure", new IOException("mock IOException"));
            slicer.handleMessage(MessageSliceReply.failed(sliceMessage.getIdentifier(), failure, sendToProbe.ref()));

            assertFailureCallback(IOException.class);

            assertFalse(slicer.hasState(sliceMessage.getIdentifier()),
                "MessageSlicer did not remove state for " + sliceMessage.getIdentifier());
        }

        LOG.info("testSlicingWithFailure ending");
    }

    @Test
    void testSliceWithFileBackedOutputStream() throws Exception {
        LOG.info("testSliceWithFileBackedOutputStream starting");

        final var message = new BytesMessage(new byte[]{1, 2, 3});
        final var fileBackedStream = streamFactory.newInstance();
        try (var out = new ObjectOutputStream(fileBackedStream)) {
            out.writeObject(message);
        }

        try (var slicer = newMessageSlicer("testSliceWithFileBackedOutputStream",
                SerializationUtils.serialize(message).length)) {
            slicer.slice(SliceOptions.builder()
                .identifier(IDENTIFIER)
                .fileBackedOutputStream(fileBackedStream)
                .sendTo(ACTOR_SYSTEM.actorSelection(sendToProbe.ref().path()))
                .replyTo(replyToProbe.ref())
                .onFailureCallback(mockOnFailureCallback)
                .build());

            final var sliceMessage = sendToProbe.expectMsgClass(MessageSlice.class);
            assembler.handleMessage(sliceMessage, sendToProbe.ref());
            assertAssembledMessage(message, replyToProbe.ref());
        }

        LOG.info("testSliceWithFileBackedOutputStream ending");
    }

    @SuppressWarnings("unchecked")
    private void testSlicing(final String logContext, final int messageSliceSize, final int expTotalSlices,
            final byte[] messageData) {
        reset(mockAssembledMessageCallback);

        final var message = new BytesMessage(messageData);

        try (var slicer = newMessageSlicer(logContext, messageSliceSize)) {
            final boolean wasSliced = slice(slicer, IDENTIFIER, message, sendToProbe.ref(), replyToProbe.ref(),
                    mockOnFailureCallback);
            assertTrue(wasSliced);

            Identifier slicingId = null;
            int expLastSliceHashCode = SlicedMessageState.INITIAL_SLICE_HASH_CODE;
            for (int sliceIndex = 1; sliceIndex <= expTotalSlices; sliceIndex++) {
                final var sliceMessage = sendToProbe.expectMsgClass(MessageSlice.class);
                slicingId = sliceMessage.getIdentifier();
                assertMessageSlice(sliceMessage, IDENTIFIER, sliceIndex, expTotalSlices, expLastSliceHashCode,
                        replyToProbe.ref());

                assembler.handleMessage(sliceMessage, sendToProbe.ref());

                final var reply = replyToProbe.expectMsgClass(MessageSliceReply.class);
                assertSuccessfulMessageSliceReply(reply, IDENTIFIER, sliceIndex);

                expLastSliceHashCode = Arrays.hashCode(sliceMessage.getData());

                slicer.handleMessage(reply);
            }

            assertAssembledMessage(message, replyToProbe.ref());

            assertFalse(slicer.hasState(slicingId), "MessageSlicer did not remove state for " + slicingId);
            assertFalse(assembler.hasState(slicingId), "MessageAssembler did not remove state for " + slicingId);
        }
    }

    private void assertFailureCallback(final Class<?> exceptionType) {
        final var exceptionCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(mockOnFailureCallback).accept(exceptionCaptor.capture());
        assertEquals(exceptionType, exceptionCaptor.getValue().getClass());
    }

    private void assertAssembledMessage(final BytesMessage message, final ActorRef sender) {
        assertAssembledMessage(mockAssembledMessageCallback, message, sender);
    }

    static void assertAssembledMessage(final BiConsumer<Object, ActorRef> mockAssembledMessageCallback,
            final BytesMessage message, final ActorRef sender) {
        final var assembledMessageCaptor = ArgumentCaptor.forClass(Object.class);
        final var senderActorRefCaptor = ArgumentCaptor.forClass(ActorRef.class);
        verify(mockAssembledMessageCallback).accept(assembledMessageCaptor.capture(), senderActorRefCaptor.capture());
        assertEquals(message, assembledMessageCaptor.getValue());
        assertEquals(sender, senderActorRefCaptor.getValue());
    }

    static void assertSuccessfulMessageSliceReply(final MessageSliceReply reply, final Identifier identifier,
            final int sliceIndex) {
        assertEquals(identifier, ((MessageSliceIdentifier) reply.getIdentifier()).getClientIdentifier());
        assertEquals(sliceIndex, reply.getSliceIndex());
    }

    static void assertFailedMessageSliceReply(final MessageSliceReply reply, final Identifier identifier,
            final boolean isRetriable) {
        assertEquals(identifier, ((MessageSliceIdentifier) reply.getIdentifier()).getClientIdentifier());
        assertTrue(reply.getFailure().isPresent());
        assertEquals(isRetriable, reply.getFailure().orElseThrow().isRetriable());
    }

    static void assertMessageSlice(final MessageSlice sliceMessage, final Identifier identifier, final int sliceIndex,
            final int totalSlices, final int lastSliceHashCode, final ActorRef replyTo) {
        assertEquals(identifier, ((MessageSliceIdentifier) sliceMessage.getIdentifier()).getClientIdentifier());
        assertEquals(sliceIndex, sliceMessage.getSliceIndex());
        assertEquals(lastSliceHashCode, sliceMessage.getLastSliceHashCode());
        assertEquals(replyTo, sliceMessage.getReplyTo());

        if (totalSlices != DONT_CARE) {
            assertEquals(totalSlices, sliceMessage.getTotalSlices());
        }
    }

    private MessageSlicer newMessageSlicer(final String logContext, final int messageSliceSize) {
        return MessageSlicer.builder()
            .messageSliceSize(messageSliceSize)
            .logContext(logContext)
            .fileBackedStreamFactory(streamFactory)
            .build();
    }
}
