/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import akka.testkit.TestProbe;
import com.google.common.io.ByteSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.io.FileBackedOutputStream;
import org.opendaylight.controller.cluster.io.FileBackedOutputStreamFactory;

/**
 * Unit tests for MessageSlicer.
 *
 * @author Thomas Pantelis
 */
public class MessageSlicerTest {
    private static final StringIdentifier IDENTIFIER = new StringIdentifier("test");
    private static final ActorSystem ACTOR_SYSTEM = ActorSystem.create("test");

    private final TestProbe testProbe = TestProbe.apply(ACTOR_SYSTEM);

    @Mock
    private FileBackedOutputStreamFactory mockFiledBackedStreamFactory;

    @Mock
    private FileBackedOutputStream mockFiledBackedStream;

    @Mock
    private Consumer<Throwable> mockOnFailureCallback;

    @Mock
    private ByteSource mockByteSource;

    @Mock
    private InputStream mockInputStream;

    @Before
    public void setup() throws IOException {
        MockitoAnnotations.initMocks(this);

        doNothing().when(mockOnFailureCallback).accept(any(Throwable.class));

        doReturn(mockFiledBackedStream).when(mockFiledBackedStreamFactory).newInstance();
        doNothing().when(mockFiledBackedStream).write(any(byte[].class), anyInt(), anyInt());
        doNothing().when(mockFiledBackedStream).write(any(byte[].class));
        doNothing().when(mockFiledBackedStream).write(anyInt());
        doNothing().when(mockFiledBackedStream).close();
        doNothing().when(mockFiledBackedStream).cleanup();
        doNothing().when(mockFiledBackedStream).flush();
        doReturn(mockByteSource).when(mockFiledBackedStream).asByteSource();

        doReturn(mockInputStream).when(mockByteSource).openStream();
        doReturn(mockInputStream).when(mockByteSource).openBufferedStream();
        doReturn(10L).when(mockByteSource).size();
    }

    @AfterClass
    public static void tearDown() {
        JavaTestKit.shutdownActorSystem(ACTOR_SYSTEM, Boolean.TRUE);
    }

    @Test
    public void testHandledMessages() {
        final MessageSliceReply reply = MessageSliceReply.success(IDENTIFIER, 1, testProbe.ref());
        assertEquals("isHandledMessage", Boolean.TRUE, MessageSlicer.isHandledMessage(reply));
        assertEquals("isHandledMessage", Boolean.FALSE, MessageSlicer.isHandledMessage(new Object()));

        final MessageSlicer slicer = new MessageSlicer(100, mockFiledBackedStreamFactory);
        assertEquals("handledMessage", Boolean.TRUE, slicer.handleMessage(reply));
        assertEquals("handledMessage", Boolean.FALSE, slicer.handleMessage(new Object()));
    }

    @Test
    public void testSliceWithFailedSerialization() throws IOException {
        IOException mockFailure = new IOException("mock IOException");
        doThrow(mockFailure).when(mockFiledBackedStream).write(any(byte[].class), anyInt(), anyInt());
        doThrow(mockFailure).when(mockFiledBackedStream).write(any(byte[].class));
        doThrow(mockFailure).when(mockFiledBackedStream).write(anyInt());
        doThrow(mockFailure).when(mockFiledBackedStream).flush();

        final MessageSlicer slicer = new MessageSlicer(100, mockFiledBackedStreamFactory);
        slicer.slice(IDENTIFIER, new BytesMessage(new byte[]{}), testProbe.ref(), testProbe.ref(),
                mockOnFailureCallback);

        assertFailureCallback(IOException.class);
        verify(mockFiledBackedStream).cleanup();
    }

    @Test
    public void testSliceWithByteSourceFailure() throws IOException {
        IOException mockFailure = new IOException("mock IOException");
        doThrow(mockFailure).when(mockByteSource).openStream();
        doThrow(mockFailure).when(mockByteSource).openBufferedStream();

        final MessageSlicer slicer = new MessageSlicer(100, mockFiledBackedStreamFactory);
        slicer.slice(IDENTIFIER, new BytesMessage(new byte[]{}), testProbe.ref(), testProbe.ref(),
                mockOnFailureCallback);

        assertFailureCallback(IOException.class);
        verify(mockFiledBackedStream).cleanup();
    }

    @Test
    public void testSliceWithInputStreamFailure() throws IOException {
        doReturn(0).when(mockInputStream).read(any(byte[].class));

        final MessageSlicer slicer = new MessageSlicer(2, mockFiledBackedStreamFactory);
        slicer.slice(IDENTIFIER, new BytesMessage(new byte[]{}), testProbe.ref(), testProbe.ref(),
                mockOnFailureCallback);

        assertFailureCallback(IOException.class);
        verify(mockFiledBackedStream).cleanup();
    }

    @Test
    public void testMessageSliceReplyWithNoState() {
        final MessageSlicer slicer = new MessageSlicer(2, mockFiledBackedStreamFactory);
        slicer.handleMessage(MessageSliceReply.success(IDENTIFIER, 1, testProbe.ref()));
        final AbortSlicing abortSlicing = testProbe.expectMsgClass(AbortSlicing.class);
        assertEquals("Identifier", IDENTIFIER, abortSlicing.getIdentifier());
    }

    private void assertFailureCallback(final Class<?> exceptionType) {
        ArgumentCaptor<Throwable> exceptionCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(mockOnFailureCallback).accept(exceptionCaptor.capture());
        assertEquals("Exception type", exceptionType, exceptionCaptor.getValue().getClass());
    }
}
