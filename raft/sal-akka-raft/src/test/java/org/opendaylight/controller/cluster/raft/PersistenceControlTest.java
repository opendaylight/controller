/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.controller.cluster.raft.persisted.VotingConfig;
import org.opendaylight.controller.cluster.raft.spi.AbstractStateCommand;
import org.opendaylight.controller.cluster.raft.spi.DisabledRaftStorage;
import org.opendaylight.controller.cluster.raft.spi.EnabledRaftStorage;
import org.opendaylight.controller.cluster.raft.spi.EntryStore.PersistCallback;

/**
 * Unit tests for RaftActorDelegatingPersistentDataProvider.
 *
 * @author Thomas Pantelis
 */
@ExtendWith(MockitoExtension.class)
class PersistenceControlTest {
    private static final VotingConfig PERSISTENT_PAYLOAD = new VotingConfig();
    private static final AbstractStateCommand NON_PERSISTENT_PAYLOAD = new AbstractStateCommand() {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        @Override
        public int size() {
            return 0;
        }

        @Override
        public int serializedSize() {
            return 0;
        }

        @Override
        protected Object writeReplace() {
            throw new UnsupportedOperationException();
        }
    };

    @Mock
    private ReplicatedLogEntry persistentLogEntry;
    @Mock
    private ReplicatedLogEntry nonPersistentLogEntry;
    @Mock
    private DisabledRaftStorage disabledStorage;
    @Mock
    private EnabledRaftStorage enabledStorage;
    @Mock
    private PersistCallback callback;

    private PersistenceControl provider;

    @BeforeEach
    void beforeEach() {
        provider = new PersistenceControl(disabledStorage, enabledStorage);
        // we should start off as DisabledRaftStorage
        assertSame(disabledStorage, provider.entryStore());
        assertSame(disabledStorage, provider.snapshotStore());
    }

    @Test
    void becomePersistentMeansEnabledStorage() {
        assertTrue(provider.becomePersistent());
        // we should become EnabledRaftStorage
        assertSame(enabledStorage, provider.entryStore());
        assertSame(enabledStorage, provider.snapshotStore());
    }

    @Test
    void testPersistWithPersistenceDisabled() throws Exception {
        doReturn(PERSISTENT_PAYLOAD).when(persistentLogEntry).command();

        doNothing().when(disabledStorage).saveVotingConfig(same(PERSISTENT_PAYLOAD), any());
        doCallRealMethod().when(disabledStorage).persistEntry(any(), any());
        disabledStorage.persistEntry(persistentLogEntry, callback);

        doReturn(NON_PERSISTENT_PAYLOAD).when(nonPersistentLogEntry).command();
        disabledStorage.persistEntry(nonPersistentLogEntry, callback);
        verify(disabledStorage).persistEntry(nonPersistentLogEntry, any());
    }
}
