/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.controller.cluster.raft.spi.DisabledRaftStorage;
import org.opendaylight.controller.cluster.raft.spi.EnabledRaftStorage;
import org.opendaylight.controller.cluster.raft.spi.RaftStorageCompleter;
import org.opendaylight.raft.spi.CompressionType;
import org.opendaylight.raft.spi.FileBackedOutputStream.Configuration;

/**
 * Unit tests for RaftActorDelegatingPersistentDataProvider.
 *
 * @author Thomas Pantelis
 */
@ExtendWith(MockitoExtension.class)
class PersistenceControlTest {
    @TempDir
    private Path directory;
    @Mock
    private RaftActor raftActor;

    private PersistenceControl control;

    @BeforeEach
    void beforeEach() {
        control = new PersistenceControl(raftActor, new RaftStorageCompleter("test", Runnable::run), directory,
            CompressionType.NONE, new Configuration(0, directory));
    }

    @Test
    void defaultIsDisabledStorage() {
        assertInstanceOf(DisabledRaftStorage.class, control.entryStore());
        assertInstanceOf(DisabledRaftStorage.class, control.snapshotStore());
    }

    @Test
    void becomePersistentMeansEnabledStorage() {
        assertTrue(control.becomePersistent());
        // we should become EnabledRaftStorage
        assertInstanceOf(EnabledRaftStorage.class, control.entryStore());
        assertInstanceOf(EnabledRaftStorage.class, control.snapshotStore());
    }
}
