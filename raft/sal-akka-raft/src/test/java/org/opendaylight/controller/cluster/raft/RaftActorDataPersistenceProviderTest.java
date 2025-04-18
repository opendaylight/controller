/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
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
import org.opendaylight.controller.cluster.raft.persisted.ClusterConfig;
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
    private static final ClusterConfig PERSISTENT_PAYLOAD = new ClusterConfig();

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
    private ArgumentCaptor<Consumer<ClusterConfig>> callbackCaptor;

    private PersistenceControl provider;

    @BeforeEach
    void beforeEach() {
        provider = new PersistenceControl(mockDisabledStorage, mockEnabledStorage);
    }

    @Test
    void testPersistWithPersistenceEnabled() {
        doReturn(true).when(mockEnabledStorage).isRecoveryApplicable();
        provider.becomePersistent();

        provider.persistEntry(mockPersistentLogEntry, mockCallback);
        verify(mockEnabledStorage).persistEntry(mockPersistentLogEntry, mockCallback);

        provider.persistEntry(mockNonPersistentLogEntry, mockCallback);
        verify(mockEnabledStorage).persistEntry(mockNonPersistentLogEntry, mockCallback);
    }

    @Test
    void testPersistWithPersistenceDisabled() {
        doReturn(false).when(mockDisabledStorage).isRecoveryApplicable();
        doReturn(PERSISTENT_PAYLOAD).when(mockPersistentLogEntry).command();
        doReturn(NON_PERSISTENT_PAYLOAD).when(mockNonPersistentLogEntry).command();

        provider.persistEntry(mockPersistentLogEntry, mockCallback);

        verify(mockEnabledStorage).persistConfig(eq(PERSISTENT_PAYLOAD), callbackCaptor.capture());
        verify(mockDisabledStorage, never()).persistEntry(mockNonPersistentLogEntry, mockCallback);
        callbackCaptor.getValue().accept(PERSISTENT_PAYLOAD);
        verify(mockCallback).accept(mockPersistentLogEntry);

        provider.persistEntry(mockNonPersistentLogEntry, mockCallback);
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
