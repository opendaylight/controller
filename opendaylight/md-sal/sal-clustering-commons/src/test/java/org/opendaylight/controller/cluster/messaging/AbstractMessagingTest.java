/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import akka.testkit.TestProbe;
import com.google.common.io.ByteSource;
import java.io.IOException;
import java.io.InputStream;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.io.FileBackedOutputStream;
import org.opendaylight.controller.cluster.io.FileBackedOutputStreamFactory;

/**
 * Abstract base class for messaging tests.
 *
 * @author Thomas Pantelis
 */
public class AbstractMessagingTest {
    protected static final StringIdentifier IDENTIFIER = new StringIdentifier("test");
    protected static ActorSystem ACTOR_SYSTEM;

    protected final TestProbe testProbe = TestProbe.apply(ACTOR_SYSTEM);

    @Mock
    protected FileBackedOutputStreamFactory mockFiledBackedStreamFactory;

    @Mock
    protected FileBackedOutputStream mockFiledBackedStream;

    @Mock
    protected ByteSource mockByteSource;

    @Mock
    protected InputStream mockInputStream;

    @BeforeClass
    public static void setupClass() throws IOException {
        ACTOR_SYSTEM = ActorSystem.create("test");
    }

    @Before
    public void setup() throws IOException {
        MockitoAnnotations.initMocks(this);

        doReturn(mockFiledBackedStream).when(mockFiledBackedStreamFactory).newInstance();
        setupMockFiledBackedStream(mockFiledBackedStream);
        doReturn(mockByteSource).when(mockFiledBackedStream).asByteSource();

        doReturn(mockInputStream).when(mockByteSource).openStream();
        doReturn(mockInputStream).when(mockByteSource).openBufferedStream();
        doReturn(10L).when(mockByteSource).size();

        doReturn(0).when(mockInputStream).read(any(byte[].class));
    }

    @AfterClass
    public static void tearDownClass() {
        JavaTestKit.shutdownActorSystem(ACTOR_SYSTEM, Boolean.TRUE);
    }

    void setupMockFiledBackedStream(final FileBackedOutputStream mockFiledBackedStream) throws IOException {
        doNothing().when(mockFiledBackedStream).write(any(byte[].class), anyInt(), anyInt());
        doNothing().when(mockFiledBackedStream).write(any(byte[].class));
        doNothing().when(mockFiledBackedStream).write(anyInt());
        doNothing().when(mockFiledBackedStream).close();
        doNothing().when(mockFiledBackedStream).cleanup();
        doNothing().when(mockFiledBackedStream).flush();
        doReturn(mockByteSource).when(mockFiledBackedStream).asByteSource();
    }
}
