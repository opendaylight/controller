/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

import java.nio.file.Path;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.controller.cluster.raft.spi.DefaultLogEntry;
import org.opendaylight.controller.cluster.raft.spi.EntryJournal;
import org.opendaylight.controller.cluster.raft.spi.EntryJournalV1;
import org.opendaylight.controller.cluster.raft.spi.LogEntry;
import org.opendaylight.raft.spi.CompressionType;

@ExtendWith(MockitoExtension.class)
class JournalRecoveryTest {
    private static final @NonNull LogEntry FIRST_ENTRY = new DefaultLogEntry(0, 1, new MockCommand("first"));
    private static final @NonNull LogEntry SECOND_ENTRY = new DefaultLogEntry(1, 2, new MockCommand("second"));
    private static final @NonNull LogEntry THIRD_ENTRY = new DefaultLogEntry(2, 3, new MockCommand("third"));

    @TempDir
    private Path stateDir;
    @Mock
    private RaftActor actor;
    @Mock
    private RaftActorSnapshotCohort<@NonNull MockSnapshotState> snapshotCohort;
    @Mock
    private RaftActorRecoveryCohort recoveryCohort;

    private EntryJournal journal;
    private JournalRecovery<?> recovery;

    @BeforeEach
    void beforeEach() throws Exception {
        journal = new EntryJournalV1("test", stateDir, CompressionType.NONE, true);
        journal.appendEntry(FIRST_ENTRY);
        journal.appendEntry(SECOND_ENTRY);
        journal.appendEntry(THIRD_ENTRY);

        doReturn("test").when(actor).memberId();
        recovery = new JournalRecovery<>(actor, snapshotCohort, recoveryCohort, new DefaultConfigParamsImpl(), journal);
    }

    @AfterEach
    void afterEach() {
        journal.close();
    }

    @Test
    void recoveryHandlesSnapshotWithOverlappingReplayOne() throws Exception {
        final var recoveryLog = new RecoveryLog("test");
        recoveryLog.setSnapshotIndex(FIRST_ENTRY.index());
        recoveryLog.setSnapshotTerm(FIRST_ENTRY.term());
        recoveryLog.setCommitIndex(FIRST_ENTRY.index());
        recoveryLog.setLastApplied(FIRST_ENTRY.index());

        recovery.recoverJournal(recoveryLog);
    }

    @Test
    void recoveryHandlesSnapshotWithOverlappingReplayTwo() throws Exception {
        final var recoveryLog = new RecoveryLog("test");
        recoveryLog.setSnapshotIndex(SECOND_ENTRY.index());
        recoveryLog.setSnapshotTerm(SECOND_ENTRY.term());
        recoveryLog.setCommitIndex(SECOND_ENTRY.index());
        recoveryLog.setLastApplied(SECOND_ENTRY.index());
        assertTrue(recoveryLog.append(THIRD_ENTRY));

        recovery.recoverJournal(recoveryLog);
    }
}
