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

import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.pekko.japi.Procedure;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.persisted.UpdateElectionTerm;
import org.opendaylight.controller.cluster.raft.spi.TermInfo;

/**
 * Unit tests for ElectionTermImpl.
 *
 * @author Thomas Pantelis
 */
@ExtendWith(MockitoExtension.class)
class PropertiesTermInfoStoreTest {
    @Mock
    private DataPersistenceProvider mockPersistence;
    @Captor
    private ArgumentCaptor<UpdateElectionTerm> message;
    @Captor
    private ArgumentCaptor<Procedure<UpdateElectionTerm>> procedure;

    private Path stateFile;

    @BeforeEach
    void beforeEach() throws Exception {
        stateFile = Files.createTempFile(PropertiesTermInfoStoreTest.class.getName(), null);
    }

    @AfterEach
    void afterEach() throws Exception {
        Files.deleteIfExists(stateFile);
    }

    @Test
    void testUpdateAndPersist() throws Exception {
        final var impl = new PropertiesTermInfoStore("test", stateFile);
        final var termInfo = new TermInfo(10, "member-1");
        impl.storeAndSetTerm(termInfo);

        assertEquals(termInfo, impl.currentTerm());

        verify(mockPersistence).persist(message.capture(), procedure.capture());

        final var update = assertInstanceOf(UpdateElectionTerm.class, message.getValue());
        assertEquals(termInfo, update.termInfo());

        procedure.getValue().apply(null);
    }
}
