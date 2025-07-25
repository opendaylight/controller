/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.persisted.ServerInfo;
import org.opendaylight.controller.cluster.raft.persisted.VotingConfig;
import org.opendaylight.raft.api.EntryInfo;
import org.opendaylight.raft.spi.CompressionType;
import org.opendaylight.raft.spi.FileBackedOutputStream.Configuration;
import org.opendaylight.raft.spi.RestrictedObjectStreams;

@ExtendWith(MockitoExtension.class)
class DisabledRaftStorageTest {
    private static final @NonNull RestrictedObjectStreams OBJECT_STREAMS =
        RestrictedObjectStreams.ofClassLoaders(DisabledRaftStorageTest.class);
    private static final VotingConfig PERSISTENT_PAYLOAD = new VotingConfig(new ServerInfo("foo", true));
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

    @TempDir
    private Path directory;
    @Mock
    private ReplicatedLogEntry logEntry;
    @Mock
    private RaftCallback<Long> callback;

    private DisabledRaftStorage storage;

    @BeforeEach
    void beforeEach() throws Exception {
        final var completer = new RaftStorageCompleter("test", Runnable::run);
        storage = new DisabledRaftStorage(completer, directory, CompressionType.NONE, new Configuration(0, directory));
        storage.start();
    }

    @AfterEach
    void afterEach() {
        storage.stop();
    }

    @Test
    void votingConfigTriggersSnapshot() throws Exception {
        doReturn(PERSISTENT_PAYLOAD).when(logEntry).command();
        storage.persistEntry(logEntry, callback);

        final var snapshot = await().atMost(Duration.ofSeconds(5)).until(storage::lastSnapshot, Objects::nonNull);
        assertNotNull(snapshot);
        assertEquals(directory, snapshot.path().getParent());
        assertEquals(EntryInfo.of(-1, -1), snapshot.lastIncluded());
        assertNull(snapshot.source());

        final var raftSnapshot = snapshot.readRaftSnapshot(OBJECT_STREAMS);
        assertEquals(List.of(), raftSnapshot.unappliedEntries());
        assertEquals(PERSISTENT_PAYLOAD, raftSnapshot.votingConfig());
    }

    @Test
    void normalPayloadCompletesImmediately() {
        doReturn(NON_PERSISTENT_PAYLOAD).when(logEntry).command();
        storage.persistEntry(logEntry, callback);
        verify(callback).invoke(null, 0L);
    }
}
