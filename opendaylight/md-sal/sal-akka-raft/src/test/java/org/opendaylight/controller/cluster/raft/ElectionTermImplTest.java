/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

import java.util.function.Consumer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.PersistentData;
import org.opendaylight.controller.cluster.raft.persisted.UpdateElectionTerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for ElectionTermImpl.
 *
 * @author Thomas Pantelis
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class ElectionTermImplTest {
    private static final Logger LOG = LoggerFactory.getLogger(RaftActorRecoverySupportTest.class);

    @Mock
    private DataPersistenceProvider mockPersistence;
    @Captor
    private ArgumentCaptor<PersistentData> message;
    @Captor
    private ArgumentCaptor<Consumer<PersistentData>> callback;

    @Test
    public void testUpdateAndPersist() throws Exception {
        ElectionTermImpl impl = new ElectionTermImpl(mockPersistence, "test", LOG);

        impl.updateAndPersist(10, "member-1");

        assertEquals("getCurrentTerm", 10, impl.getCurrentTerm());
        assertEquals("getVotedFor", "member-1", impl.getVotedFor());

        verify(mockPersistence).persist(message.capture(), callback.capture());

        assertEquals("Message type", UpdateElectionTerm.class, message.getValue().getClass());
        UpdateElectionTerm update = (UpdateElectionTerm)message.getValue();
        assertEquals("getCurrentTerm", 10, update.getCurrentTerm());
        assertEquals("getVotedFor", "member-1", update.getVotedFor());

        callback.getValue().accept(null);
    }
}
