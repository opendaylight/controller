/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.apache.pekko.persistence.RecoveryCompleted;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.controller.cluster.raft.persisted.UpdateElectionTerm;
import org.opendaylight.controller.cluster.raft.persisted.VotingConfig;
import org.opendaylight.controller.cluster.raft.spi.EnabledRaftStorage;
import org.opendaylight.controller.cluster.raft.spi.LogEntry;
import org.opendaylight.controller.cluster.raft.spi.RaftStorageCompleter;
import org.opendaylight.raft.api.EntryInfo;
import org.opendaylight.raft.spi.CompressionType;
import org.opendaylight.raft.spi.FileBackedOutputStream.Configuration;
import org.opendaylight.raft.spi.RestrictedObjectStreams;

@ExtendWith(MockitoExtension.class)
class PekkoRecoveryTest {
    @TempDir
    private Path stateDir;
    @Mock
    private RaftActor raftActor;
    @Mock
    private RaftActorSnapshotCohort<@NonNull MockSnapshotState> snapshotCohort;
    @Mock
    private RaftActorRecoveryCohort recoveryCohort;
    @Mock
    private PersistenceProvider persistenceProvider;

    private LocalAccess localAccess;
    private EnabledRaftStorage raftStorage;
    private PekkoRecovery<@NonNull MockSnapshotState> recovery;

    @BeforeEach
    void beforeEach() throws Exception {
        localAccess = new LocalAccess("test", stateDir);
        raftStorage = new EnabledRaftStorage(new RaftStorageCompleter("test", Runnable::run), stateDir,
            CompressionType.NONE, new Configuration(0, stateDir), true);

        doReturn("test").when(raftActor).memberId();
        doReturn(localAccess).when(raftActor).localAccess();
        doReturn(persistenceProvider).when(raftActor).persistence();
        doReturn(raftStorage).when(persistenceProvider).snapshotStore();
        doReturn(MockSnapshotState.SUPPORT).when(snapshotCohort).support();
        doReturn(new PeerInfos("test", Map.of())).when(raftActor).peerInfos();

        recovery = new PekkoRecovery<>(raftActor, snapshotCohort, recoveryCohort, new DefaultConfigParamsImpl());
    }

    @Test
    void updateElectionTermPropagatesToStore() throws Exception {
        assertNull(recovery.handleRecoveryMessage(new UpdateElectionTerm(1, null)));

        doReturn(1L).when(raftActor).lastSequenceNr();
        final var result = assertFinishRecovery();
        assertFalse(result.canRestoreFromSnapshot());

        assertNull(assertSnapshot(-1, -1, null));
    }

    private @NonNull RecoveryResult assertFinishRecovery() {
        final var result = recovery.handleRecoveryMessage(RecoveryCompleted.getInstance());
        assertNotNull(result);
        return result;
    }

    private @Nullable MockSnapshotState assertSnapshot(long snapshotIndex, long snapshotTerm,
            final VotingConfig votingConfig, final LogEntry... entries) throws Exception {
        final var snapshot = raftStorage.lastSnapshot();
        assertNotNull(snapshot);
        assertEquals(EntryInfo.of(snapshotIndex, snapshotTerm), snapshot.lastIncluded());

        final var raftSnapshot = snapshot.readRaftSnapshot(
            RestrictedObjectStreams.ofClassLoaders(PekkoRecoveryTest.class));
        assertEquals(votingConfig, raftSnapshot.votingConfig());
        assertEquals(List.of(entries), raftSnapshot.unappliedEntries());

        return snapshot.readSnapshot(snapshotCohort.support().reader());
    }
}
