/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.verify;

import org.apache.pekko.japi.Procedure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.persisted.UpdateElectionTerm;
import org.slf4j.Logger;

/**
 * Unit tests for ElectionTermImpl.
 *
 * @author Thomas Pantelis
 */
@ExtendWith(MockitoExtension.class)
class ElectionTermImplTest {
    @Mock
    private DataPersistenceProvider mockPersistence;
    @Mock
    private Logger log;
    @Captor
    private ArgumentCaptor<UpdateElectionTerm> message;
    @Captor
    private ArgumentCaptor<Procedure<UpdateElectionTerm>> procedure;

    @Test
    void testUpdateAndPersist() throws Exception {
        final var impl = new ElectionTermImpl(mockPersistence, "test", log);
        final var termInfo = new TermInfo(10, "member-1");
        impl.updateAndPersist(termInfo);

        assertEquals(termInfo, impl.currentTerm());

        verify(mockPersistence).persist(message.capture(), procedure.capture());

        final var update = assertInstanceOf(UpdateElectionTerm.class, message.getValue());
        assertEquals(termInfo, update.termInfo());

        procedure.getValue().apply(null);
    }
}
