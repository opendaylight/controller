/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import akka.japi.Procedure;
import com.google.common.util.concurrent.SettableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.base.messages.UpdateElectionTerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for ElectionTermImpl.
 *
 * @author Thomas Pantelis
 */
public class ElectionTermImplTest {
    private static final Logger LOG = LoggerFactory.getLogger(RaftActorRecoverySupportTest.class);

    @Mock
    private DataPersistenceProvider mockPersistence;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Exception {
                final Object[] args = invocation.getArguments();
                ((Procedure<Object>)args[1]).apply(args[0]);
                return null;
            }
        }).when(mockPersistence).persist(any(Object.class), any(Procedure.class));
    }

    @Test
    public void testUpdateAndPersist() throws Exception {
        final ElectionTermImpl impl = new ElectionTermImpl(mockPersistence, "test", LOG);

        final SettableFuture<Void> p = SettableFuture.create();
        impl.updateAndPersist(10, "member-1", new Procedure<Void>() {
            @Override
            public void apply(final Void param) throws Exception {
                p.set(null);
            }
        });

        // Wait up to 10 seconds for callback to execute
        p.get(10, TimeUnit.SECONDS);

        assertEquals("getCurrentTerm", 10, impl.getCurrentTerm());
        assertEquals("getVotedFor", "member-1", impl.getVotedFor());

        ArgumentCaptor<Object> message = ArgumentCaptor.forClass(Object.class);
        @SuppressWarnings({ "unchecked", "rawtypes" })
        ArgumentCaptor<Procedure<Object>> procedure =
                (ArgumentCaptor)ArgumentCaptor.forClass(Procedure.class);
        verify(mockPersistence).persist(message.capture(), procedure.capture());

        assertEquals("Message type", UpdateElectionTerm.class, message.getValue().getClass());
        UpdateElectionTerm update = (UpdateElectionTerm)message.getValue();
        assertEquals("getCurrentTerm", 10, update.getCurrentTerm());
        assertEquals("getVotedFor", "member-1", update.getVotedFor());

        procedure.getValue().apply(update);
    }
}
