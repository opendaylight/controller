/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.controller.cluster.raft.ReplicatedLog.RecoveringApplied;
import org.opendaylight.controller.cluster.raft.ReplicatedLog.RecoveringPosition;
import org.opendaylight.controller.cluster.raft.ReplicatedLog.RecoveringUnapplied;
import org.opendaylight.raft.api.EntryInfo;
import org.opendaylight.raft.spi.CompressionType;

@ExtendWith(MockitoExtension.class)
class EntryJournalTest {
    @Mock
    private RecoveringPosition recoverPos;
    @Mock
    private RecoveringApplied recoverApplied;
    @Mock
    private RecoveringUnapplied recoverUnapplied;
    @TempDir
    private Path directory;

    private EntryJournalV1 journal;

    @BeforeEach
    void beforeEach() throws Exception {
        journal = new EntryJournalV1("test", directory, CompressionType.NONE, false);
    }

    @AfterEach
    void afterEach() {
        journal.close();
    }

    @Test
    void recoverEmpty() throws Exception {
        doReturn(recoverApplied).when(recoverPos).recoverPosition(anyLong(), any());
        doReturn(recoverUnapplied).when(recoverApplied).finish();
        doNothing().when(recoverUnapplied).finish();

        journal.recoverTo(recoverPos, EntryInfo.of(2, 2));

        verify(recoverPos).recoverPosition(-1, EntryInfo.of(2, 2));
    }
}
