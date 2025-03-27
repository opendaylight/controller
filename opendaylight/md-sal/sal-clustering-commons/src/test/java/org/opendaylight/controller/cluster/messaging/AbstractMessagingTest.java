/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

import com.google.common.io.ByteSource;
import java.io.IOException;
import java.io.InputStream;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.testkit.TestProbe;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.raft.spi.FileBackedOutputStream;
import org.opendaylight.raft.spi.FileBackedOutputStreamFactory;

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
    public static void setupClass() {
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
        TestKit.shutdownActorSystem(ACTOR_SYSTEM, true);
    }

    void setupMockFiledBackedStream(final FileBackedOutputStream mockOutputStream) throws IOException {
        doNothing().when(mockOutputStream).write(any(byte[].class), anyInt(), anyInt());
        doNothing().when(mockOutputStream).write(any(byte[].class));
        doNothing().when(mockOutputStream).write(anyInt());
        doNothing().when(mockOutputStream).close();
        doNothing().when(mockOutputStream).cleanup();
        doNothing().when(mockOutputStream).flush();
        doReturn(mockByteSource).when(mockOutputStream).asByteSource();
    }
}
