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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.opendaylight.controller.cluster.messaging.MessageSlicingIntegrationTest.assertAssembledMessage;
import static org.opendaylight.controller.cluster.messaging.MessageSlicingIntegrationTest.assertFailedMessageSliceReply;
import static org.opendaylight.controller.cluster.messaging.MessageSlicingIntegrationTest.assertSuccessfulMessageSliceReply;

import com.google.common.util.concurrent.Uninterruptibles;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.pekko.actor.ActorRef;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.cluster.io.FileBackedOutputStream;
import org.opendaylight.controller.cluster.messaging.MessageAssembler.Builder;

/**
 * Unit tests for MessageAssembler.
 *
 * @author Thomas Pantelis
 */
public class MessageAssemblerTest extends AbstractMessagingTest {

    @Mock
    private BiConsumer<Object, ActorRef> mockAssembledMessageCallback;

    @Override
    @Before
    public void setup() throws IOException {
        super.setup();

        doNothing().when(mockAssembledMessageCallback).accept(any(Object.class), any(ActorRef.class));
    }

    @Test
    public void testHandledMessages() {
        final MessageSlice messageSlice = new MessageSlice(IDENTIFIER, new byte[0], 1, 1, 1, testProbe.ref());
        final AbortSlicing abortSlicing = new AbortSlicing(IDENTIFIER);
        assertEquals("isHandledMessage", Boolean.TRUE, MessageAssembler.isHandledMessage(messageSlice));
        assertEquals("isHandledMessage", Boolean.TRUE, MessageAssembler.isHandledMessage(abortSlicing));
        assertEquals("isHandledMessage", Boolean.FALSE, MessageSlicer.isHandledMessage(new Object()));

        try (MessageAssembler assembler = newMessageAssembler("testHandledMessages")) {
            assertEquals("handledMessage", Boolean.TRUE, assembler.handleMessage(messageSlice, testProbe.ref()));
            assertEquals("handledMessage", Boolean.TRUE, assembler.handleMessage(abortSlicing, testProbe.ref()));
            assertEquals("handledMessage", Boolean.FALSE, assembler.handleMessage(new Object(), testProbe.ref()));
        }
    }

    @Test
    public void testSingleMessageSlice() {
        try (var assembler = newMessageAssembler("testSingleMessageSlice")) {
            final var fileBackStream = spy(new FileBackedOutputStream(100000000));
            doReturn(fileBackStream).when(mockFiledBackedStreamFactory).newInstance();

            final MessageSliceIdentifier identifier = new MessageSliceIdentifier(IDENTIFIER, 1);
            final BytesMessage message = new BytesMessage(new byte[]{1, 2, 3});

            final MessageSlice messageSlice = new MessageSlice(identifier, SerializationUtils.serialize(message), 1, 1,
                    SlicedMessageState.INITIAL_SLICE_HASH_CODE, testProbe.ref());
            assembler.handleMessage(messageSlice, testProbe.ref());

            final MessageSliceReply reply = testProbe.expectMsgClass(MessageSliceReply.class);
            assertSuccessfulMessageSliceReply(reply, IDENTIFIER, 1);

            assertAssembledMessage(mockAssembledMessageCallback, message, testProbe.ref());

            assertFalse("MessageAssembler did not remove state for " + identifier, assembler.hasState(identifier));
            verify(fileBackStream).cleanup();
        }
    }

    @Test
    public void testMessageSliceWithByteSourceFailure() throws IOException {
        try (MessageAssembler assembler = newMessageAssembler("testMessageSliceWithByteSourceFailure")) {
            IOException mockFailure = new IOException("mock IOException");
            doThrow(mockFailure).when(mockByteSource).openStream();
            doThrow(mockFailure).when(mockByteSource).openBufferedStream();

            final MessageSliceIdentifier identifier = new MessageSliceIdentifier(IDENTIFIER, 1);
            final BytesMessage message = new BytesMessage(new byte[]{1, 2, 3});

            final MessageSlice messageSlice = new MessageSlice(identifier, SerializationUtils.serialize(message), 1, 1,
                    SlicedMessageState.INITIAL_SLICE_HASH_CODE, testProbe.ref());
            assembler.handleMessage(messageSlice, testProbe.ref());

            final MessageSliceReply reply = testProbe.expectMsgClass(MessageSliceReply.class);
            assertFailedMessageSliceReply(reply, IDENTIFIER, false);
            assertEquals("Failure cause", mockFailure, reply.getFailure().orElseThrow().getCause());

            assertFalse("MessageAssembler did not remove state for " + identifier, assembler.hasState(identifier));
            verify(mockFiledBackedStream).cleanup();
        }
    }

    @Test
    public void testMessageSliceWithStreamWriteFailure() throws IOException {
        try (MessageAssembler assembler = newMessageAssembler("testMessageSliceWithStreamWriteFailure")) {
            IOException mockFailure = new IOException("mock IOException");
            doThrow(mockFailure).when(mockFiledBackedStream).write(any(byte[].class), anyInt(), anyInt());
            doThrow(mockFailure).when(mockFiledBackedStream).write(any(byte[].class));
            doThrow(mockFailure).when(mockFiledBackedStream).write(anyInt());
            doThrow(mockFailure).when(mockFiledBackedStream).flush();

            final MessageSliceIdentifier identifier = new MessageSliceIdentifier(IDENTIFIER, 1);
            final BytesMessage message = new BytesMessage(new byte[]{1, 2, 3});

            final MessageSlice messageSlice = new MessageSlice(identifier, SerializationUtils.serialize(message), 1, 1,
                    SlicedMessageState.INITIAL_SLICE_HASH_CODE, testProbe.ref());
            assembler.handleMessage(messageSlice, testProbe.ref());

            final MessageSliceReply reply = testProbe.expectMsgClass(MessageSliceReply.class);
            assertFailedMessageSliceReply(reply, IDENTIFIER, false);
            assertEquals("Failure cause", mockFailure, reply.getFailure().orElseThrow().getCause());

            assertFalse("MessageAssembler did not remove state for " + identifier, assembler.hasState(identifier));
            verify(mockFiledBackedStream).cleanup();
        }
    }

    @Test
    public void testAssembledMessageStateExpiration() {
        final int expiryDuration = 200;
        try (MessageAssembler assembler = newMessageAssemblerBuilder("testAssembledMessageStateExpiration")
                .expireStateAfterInactivity(expiryDuration, TimeUnit.MILLISECONDS).build()) {
            final MessageSliceIdentifier identifier = new MessageSliceIdentifier(IDENTIFIER, 1);
            final BytesMessage message = new BytesMessage(new byte[]{1, 2, 3});

            final MessageSlice messageSlice = new MessageSlice(identifier, SerializationUtils.serialize(message), 1, 2,
                    SlicedMessageState.INITIAL_SLICE_HASH_CODE, testProbe.ref());
            assembler.handleMessage(messageSlice, testProbe.ref());

            final MessageSliceReply reply = testProbe.expectMsgClass(MessageSliceReply.class);
            assertSuccessfulMessageSliceReply(reply, IDENTIFIER, 1);

            assertTrue("MessageAssembler should have remove state for " + identifier, assembler.hasState(identifier));
            Uninterruptibles.sleepUninterruptibly(expiryDuration + 50, TimeUnit.MILLISECONDS);
            assertFalse("MessageAssembler did not remove state for " + identifier, assembler.hasState(identifier));

            verify(mockFiledBackedStream).cleanup();
        }
    }

    @Test
    public void testFirstMessageSliceWithInvalidIndex() {
        try (MessageAssembler assembler = newMessageAssembler("testFirstMessageSliceWithInvalidIndex")) {
            final MessageSliceIdentifier identifier = new MessageSliceIdentifier(IDENTIFIER, 1);
            final MessageSlice messageSlice = new MessageSlice(identifier, new byte[0], 2, 3, 1, testProbe.ref());
            assembler.handleMessage(messageSlice, testProbe.ref());

            final MessageSliceReply reply = testProbe.expectMsgClass(MessageSliceReply.class);
            assertFailedMessageSliceReply(reply, IDENTIFIER, true);
            assertFalse("MessageAssembler should not have state for " + identifier, assembler.hasState(identifier));
        }
    }

    private MessageAssembler newMessageAssembler(final String logContext) {
        return newMessageAssemblerBuilder(logContext).build();
    }

    private Builder newMessageAssemblerBuilder(final String logContext) {
        return MessageAssembler.builder().fileBackedStreamFactory(mockFiledBackedStreamFactory)
                .assembledMessageCallback(mockAssembledMessageCallback).logContext(logContext);
    }
}
