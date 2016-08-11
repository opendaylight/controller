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
import akka.japi.Procedure;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.persisted.UpdateElectionTerm;
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

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testUpdateAndPersist() throws Exception {
        ElectionTermImpl impl = new ElectionTermImpl(mockPersistence, "test", LOG);

        impl.updateAndPersist(10, "member-1");

        assertEquals("getCurrentTerm", 10, impl.getCurrentTerm());
        assertEquals("getVotedFor", "member-1", impl.getVotedFor());

        ArgumentCaptor<Object> message = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Procedure> procedure = ArgumentCaptor.forClass(Procedure.class);
        verify(mockPersistence).persist(message.capture(), procedure.capture());

        assertEquals("Message type", UpdateElectionTerm.class, message.getValue().getClass());
        UpdateElectionTerm update = (UpdateElectionTerm)message.getValue();
        assertEquals("getCurrentTerm", 10, update.getCurrentTerm());
        assertEquals("getVotedFor", "member-1", update.getVotedFor());

        procedure.getValue().apply(null);
    }
}
