/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.controller.cluster.raft.messages.Payload;
import org.opendaylight.controller.cluster.raft.persisted.VotingConfig;
import org.opendaylight.controller.cluster.raft.spi.AbstractStateCommand;
import org.opendaylight.controller.cluster.raft.spi.DisabledRaftStorage;
import org.opendaylight.controller.cluster.raft.spi.EnabledRaftStorage;

/**
 * Unit tests for RaftActorDelegatingPersistentDataProvider.
 *
 * @author Thomas Pantelis
 */
@ExtendWith(MockitoExtension.class)
class RaftActorDataPersistenceProviderTest {
    private static final VotingConfig PERSISTENT_PAYLOAD = new VotingConfig();

    private static final Payload NON_PERSISTENT_PAYLOAD = new TestNonPersistentPayload();

    @Mock
    private ReplicatedLogEntry mockPersistentLogEntry;
    @Mock
    private ReplicatedLogEntry mockNonPersistentLogEntry;
    @Mock
    private DisabledRaftStorage mockDisabledStorage;
    @Mock
    private EnabledRaftStorage mockEnabledStorage;
    @Mock
    private Consumer<ReplicatedLogEntry> mockCallback;
    @Captor
    private ArgumentCaptor<Consumer<VotingConfig>> callbackCaptor;

    private PersistenceControl provider;

    @BeforeEach
    void beforeEach() {
        provider = new PersistenceControl(mockDisabledStorage, mockEnabledStorage);
    }

    @Test
    void testPersistWithPersistenceEnabled() throws Exception {
        provider.becomePersistent();

        provider.persistEntry(mockPersistentLogEntry);
        verify(mockEnabledStorage).persistEntry(mockPersistentLogEntry);

        provider.persistEntry(mockNonPersistentLogEntry);
        verify(mockEnabledStorage).persistEntry(mockNonPersistentLogEntry);
    }

    @Test
    void testPersistWithPersistenceDisabled() throws Exception {
        doReturn(PERSISTENT_PAYLOAD).when(mockPersistentLogEntry).command();

        doNothing().when(mockDisabledStorage).saveVotingConfig(same(PERSISTENT_PAYLOAD), any());
        doCallRealMethod().when(mockDisabledStorage).persistEntry(any(), any());
        provider.persistEntry(mockPersistentLogEntry);

        doReturn(NON_PERSISTENT_PAYLOAD).when(mockNonPersistentLogEntry).command();
        provider.persistEntry(mockNonPersistentLogEntry);
        verify(mockDisabledStorage).persistEntry(mockNonPersistentLogEntry, mockCallback);
    }

    static class TestNonPersistentPayload extends AbstractStateCommand {
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
            // Not needed
            throw new UnsupportedOperationException();
        }
    }
}
