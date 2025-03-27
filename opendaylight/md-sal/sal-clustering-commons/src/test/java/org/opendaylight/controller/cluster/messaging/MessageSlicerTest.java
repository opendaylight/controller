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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.google.common.util.concurrent.Uninterruptibles;
import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.pekko.actor.ActorRef;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.opendaylight.raft.spi.FileBackedOutputStream;
import org.opendaylight.yangtools.concepts.Identifier;

/**
 * Unit tests for MessageSlicer.
 *
 * @author Thomas Pantelis
 */
public class MessageSlicerTest extends AbstractMessagingTest {
    @Mock
    private Consumer<Throwable> mockOnFailureCallback;

    @Override
    @Before
    public void setup() throws IOException {
        super.setup();

        doNothing().when(mockOnFailureCallback).accept(any(Throwable.class));
    }

    @Test
    public void testHandledMessages() {
        try (MessageSlicer slicer = newMessageSlicer("testHandledMessages", 100)) {
            MessageSliceIdentifier messageSliceId = new MessageSliceIdentifier(IDENTIFIER, slicer.getId());
            final MessageSliceReply reply = MessageSliceReply.success(messageSliceId, 1, testProbe.ref());
            assertEquals("isHandledMessage", Boolean.TRUE, MessageSlicer.isHandledMessage(reply));
            assertEquals("isHandledMessage", Boolean.FALSE, MessageSlicer.isHandledMessage(new Object()));

            assertEquals("handledMessage", Boolean.TRUE, slicer.handleMessage(reply));
            assertEquals("handledMessage", Boolean.FALSE, slicer.handleMessage(new Object()));
            assertEquals("handledMessage", Boolean.FALSE, slicer.handleMessage(MessageSliceReply.success(
                    IDENTIFIER, 1,testProbe.ref())));
            assertEquals("handledMessage", Boolean.FALSE, slicer.handleMessage(MessageSliceReply.success(
                    new MessageSliceIdentifier(IDENTIFIER, slicer.getId() + 1), 1,testProbe.ref())));
        }
    }

    @Test
    public void testSliceWithFailedSerialization() throws IOException {
        IOException mockFailure = new IOException("mock IOException");
        doThrow(mockFailure).when(mockFiledBackedStream).write(any(byte[].class), anyInt(), anyInt());
        doThrow(mockFailure).when(mockFiledBackedStream).write(any(byte[].class));
        doThrow(mockFailure).when(mockFiledBackedStream).write(anyInt());
        doThrow(mockFailure).when(mockFiledBackedStream).flush();

        try (MessageSlicer slicer = newMessageSlicer("testSliceWithFailedSerialization", 100)) {
            final boolean wasSliced = slice(slicer, IDENTIFIER, new BytesMessage(new byte[]{}), testProbe.ref(),
                    testProbe.ref(), mockOnFailureCallback);
            assertFalse(wasSliced);

            assertFailureCallback(IOException.class);
            verify(mockFiledBackedStream).cleanup();
        }
    }

    @Test
    public void testSliceWithByteSourceFailure() throws IOException {
        IOException mockFailure = new IOException("mock IOException");
        doThrow(mockFailure).when(mockByteSource).openStream();
        doThrow(mockFailure).when(mockByteSource).openBufferedStream();

        try (MessageSlicer slicer = newMessageSlicer("testSliceWithByteSourceFailure", 100)) {
            final boolean wasSliced = slice(slicer, IDENTIFIER, new BytesMessage(new byte[]{}), testProbe.ref(),
                    testProbe.ref(), mockOnFailureCallback);
            assertFalse(wasSliced);

            assertFailureCallback(IOException.class);
            verify(mockFiledBackedStream).cleanup();
        }
    }

    @Test
    public void testSliceWithInputStreamFailure() throws IOException {
        doReturn(0).when(mockInputStream).read(any(byte[].class));

        try (MessageSlicer slicer = newMessageSlicer("testSliceWithInputStreamFailure", 2)) {
            final boolean wasSliced = slice(slicer, IDENTIFIER, new BytesMessage(new byte[]{}), testProbe.ref(),
                    testProbe.ref(), mockOnFailureCallback);
            assertFalse(wasSliced);

            assertFailureCallback(IOException.class);
            verify(mockFiledBackedStream).cleanup();
        }
    }

    @Test
    public void testMessageSliceReplyWithNoState() {
        try (MessageSlicer slicer = newMessageSlicer("testMessageSliceReplyWithNoState", 1000)) {
            MessageSliceIdentifier messageSliceId = new MessageSliceIdentifier(IDENTIFIER, slicer.getId());
            slicer.handleMessage(MessageSliceReply.success(messageSliceId, 1, testProbe.ref()));
            final AbortSlicing abortSlicing = testProbe.expectMsgClass(AbortSlicing.class);
            assertEquals("Identifier", messageSliceId, abortSlicing.getIdentifier());
        }
    }

    @Test
    public void testCloseAllSlicedMessageState() throws IOException {
        doReturn(1).when(mockInputStream).read(any(byte[].class));

        final MessageSlicer slicer = newMessageSlicer("testCloseAllSlicedMessageState", 1);
        slice(slicer, IDENTIFIER, new BytesMessage(new byte[]{1, 2}), testProbe.ref(), testProbe.ref(),
                mockOnFailureCallback);

        slicer.close();

        verify(mockFiledBackedStream).cleanup();
        verifyNoMoreInteractions(mockOnFailureCallback);
    }

    @Test
    public void testCancelSlicing() throws IOException {
        doReturn(1).when(mockInputStream).read(any(byte[].class));

        final MessageSlicer slicer = newMessageSlicer("testCloseAllSlicedMessageState", 1);
        slicer.slice(SliceOptions.builder().identifier(IDENTIFIER).fileBackedOutputStream(mockFiledBackedStream)
                .sendTo(testProbe.ref()).replyTo(testProbe.ref()).onFailureCallback(mockOnFailureCallback).build());

        final FileBackedOutputStream mockFiledBackedStream2 = mock(FileBackedOutputStream.class);
        setupMockFiledBackedStream(mockFiledBackedStream2);
        slicer.slice(SliceOptions.builder().identifier(new StringIdentifier("test2"))
                .fileBackedOutputStream(mockFiledBackedStream2).sendTo(testProbe.ref()).replyTo(testProbe.ref())
                .onFailureCallback(mockOnFailureCallback).build());

        slicer.cancelSlicing(id -> id.equals(IDENTIFIER));

        verify(mockFiledBackedStream).cleanup();
        verify(mockFiledBackedStream2, never()).cleanup();
        verifyNoMoreInteractions(mockOnFailureCallback);
    }

    @Test
    public void testCheckExpiredSlicedMessageState() throws IOException {
        doReturn(1).when(mockInputStream).read(any(byte[].class));

        final int expiryDuration = 200;
        try (MessageSlicer slicer = MessageSlicer.builder().messageSliceSize(1)
                .logContext("testCheckExpiredSlicedMessageState")
                .fileBackedStreamFactory(mockFiledBackedStreamFactory)
                .expireStateAfterInactivity(expiryDuration, TimeUnit.MILLISECONDS).build()) {
            slice(slicer, IDENTIFIER, new BytesMessage(new byte[]{1, 2}), testProbe.ref(), testProbe.ref(),
                    mockOnFailureCallback);

            Uninterruptibles.sleepUninterruptibly(expiryDuration + 50, TimeUnit.MILLISECONDS);
            slicer.checkExpiredSlicedMessageState();

            assertFailureCallback(RuntimeException.class);
            verify(mockFiledBackedStream).cleanup();
        }
    }

    private void assertFailureCallback(final Class<?> exceptionType) {
        ArgumentCaptor<Throwable> exceptionCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(mockOnFailureCallback).accept(exceptionCaptor.capture());
        assertEquals("Exception type", exceptionType, exceptionCaptor.getValue().getClass());
    }

    private MessageSlicer newMessageSlicer(String logContext, final int messageSliceSize) {
        return MessageSlicer.builder().messageSliceSize(messageSliceSize).logContext(logContext)
                .fileBackedStreamFactory(mockFiledBackedStreamFactory).build();
    }

    static boolean slice(MessageSlicer slicer, Identifier identifier, Serializable message, ActorRef sendTo,
            ActorRef replyTo, Consumer<Throwable> onFailureCallback) {
        return slicer.slice(SliceOptions.builder().identifier(identifier).message(message).sendTo(sendTo)
                .replyTo(replyTo).onFailureCallback(onFailureCallback).build());
    }
}
