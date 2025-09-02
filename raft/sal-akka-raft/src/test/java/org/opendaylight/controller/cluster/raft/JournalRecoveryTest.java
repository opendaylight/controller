/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;

import java.nio.file.Path;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.controller.cluster.raft.spi.DefaultLogEntry;
import org.opendaylight.controller.cluster.raft.spi.EntryJournal;
import org.opendaylight.controller.cluster.raft.spi.EntryJournalV1;
import org.opendaylight.raft.spi.CompressionType;
//import org.opendaylight.raft.spi.RestrictedObjectStreams;

@ExtendWith(MockitoExtension.class)
class JournalRecoveryTest {
    private static final @NonNull DefaultLogEntry FIRST_ENTRY = new DefaultLogEntry(0, 1, new MockCommand("first"));
    private static final @NonNull DefaultLogEntry SECOND_ENTRY = new DefaultLogEntry(1, 2, new MockCommand("second"));
    private static final @NonNull DefaultLogEntry THIRD_ENTRY = new DefaultLogEntry(2, 3, new MockCommand("third"));

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
        journal.setApplyTo(1);

        doReturn("test").when(actor).memberId();
        recovery = new JournalRecovery<>(actor, snapshotCohort, recoveryCohort, new DefaultConfigParamsImpl(), journal);
    }

    @AfterEach
    void afterEach() {
        journal.close();
    }

//    @Test
//    void recoveryHandlesSnapshotWithOverlappingReplayOne() throws Exception {
//        final var input = new RecoveryLog("test");
//        input.setSnapshotIndex(FIRST_ENTRY.index());
//        input.setSnapshotTerm(FIRST_ENTRY.term());
//        input.setCommitIndex(FIRST_ENTRY.index());
//        input.setLastApplied(FIRST_ENTRY.index());
//
//        doReturn(RestrictedObjectStreams.ofClassLoaders(JournalRecoveryTest.class)).when(actor).objectStreams();
//        doCallRealMethod().when(actor).recoveryObserver();
//
//        final var output = recovery.recoverJournal(new RecoveryResult(input, true)).recoveryLog();
//        assertEquals(0, output.getSnapshotIndex());
//        assertEquals(1, output.getSnapshotTerm());
//        assertEquals(2, output.size());
//        assertEquals(SECOND_ENTRY, DefaultLogEntry.of(output.entryAt(0)));
//        assertEquals(THIRD_ENTRY, DefaultLogEntry.of(output.entryAt(1)));
//        assertEquals(1, journal.applyToJournalIndex());
//    }
//
//    @Test
//    void recoveryHandlesSnapshotWithOverlappingReplayTwo() throws Exception {
//        final var input = new RecoveryLog("test");
//        input.setSnapshotIndex(SECOND_ENTRY.index());
//        input.setSnapshotTerm(SECOND_ENTRY.term());
//        input.setCommitIndex(SECOND_ENTRY.index());
//        input.setLastApplied(SECOND_ENTRY.index());
//        assertTrue(input.append(THIRD_ENTRY));
//
//        doCallRealMethod().when(actor).recoveryObserver();
//
//        final var output = recovery.recoverJournal(new RecoveryResult(input, true)).recoveryLog();
//        assertEquals(1, output.getSnapshotIndex());
//        assertEquals(2, output.getSnapshotTerm());
//        assertEquals(1, output.size());
//        assertEquals(THIRD_ENTRY, DefaultLogEntry.of(output.entryAt(0)));
//
//        // FIXME: We currently trim the journal an re-apply entries. We really should skip over same entries -- and
//        //        retain the applyIndex.
//        assertEquals(0, journal.applyToJournalIndex());
//    }
}
