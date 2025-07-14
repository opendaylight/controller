/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.OptionalInt;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.pekko.protobufv3.internal.ByteString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.controller.cluster.raft.behaviors.SnapshotTracker.InvalidChunkException;
import org.opendaylight.raft.spi.CompressionType;
import org.opendaylight.raft.spi.FileBackedOutputStream;
import org.opendaylight.raft.spi.FileBackedOutputStream.Configuration;

@ExtendWith(MockitoExtension.class)
class SnapshotTrackerTest {
    private final HashMap<String, String> data = new HashMap<>();

    @TempDir
    private Path tempDir;

    private FileBackedOutputStream fbos;
    private ByteString byteString;
    private byte[] chunk1;
    private byte[] chunk2;
    private byte[] chunk3;

    @BeforeEach
    void beforeEach() {
        data.put("key1", "value1");
        data.put("key2", "value2");
        data.put("key3", "value3");

        byteString = ByteString.copyFrom(SerializationUtils.serialize(data));
        chunk1 = getNextChunk(byteString, 0, 10);
        chunk2 = getNextChunk(byteString, 10, 10);
        chunk3 = getNextChunk(byteString, 20, byteString.size());

        fbos = spy(new FileBackedOutputStream(new Configuration(100000000, tempDir)));
    }

    @Test
    void testAddChunks() throws Exception {
        try (var tracker = new SnapshotTracker("test", 3, "leader", fbos, CompressionType.NONE)) {
            tracker.addChunk(1, chunk1, OptionalInt.of(LeaderInstallSnapshotState.INITIAL_LAST_CHUNK_HASH_CODE));
            tracker.addChunk(2, chunk2, OptionalInt.of(Arrays.hashCode(chunk1)));
            tracker.addChunk(3, chunk3, OptionalInt.of(Arrays.hashCode(chunk2)));

            final var snapshotBytes = tracker.toStreamSource();
            assertEquals(data, SerializationUtils.deserialize(snapshotBytes.openStream().readAllBytes()));
        }

        verify(fbos).cleanup();
    }

    @Test
    void testAddChunkWhenAlreadySealed() throws Exception {
        try (var tracker = new SnapshotTracker("test", 2, "leader", fbos, CompressionType.NONE)) {
            tracker.addChunk(1, chunk1, OptionalInt.empty());
            tracker.addChunk(2, chunk2, OptionalInt.empty());
            assertThrows(InvalidChunkException.class, () -> tracker.addChunk(3, chunk3, OptionalInt.empty()));
        }
    }

    @Test
    void testInvalidFirstChunkIndex() throws Exception {
        try (var tracker = new SnapshotTracker("test", 2, "leader", fbos, CompressionType.NONE)) {
            assertThrows(InvalidChunkException.class,
                () -> tracker.addChunk(LeaderInstallSnapshotState.FIRST_CHUNK_INDEX - 1, chunk1, OptionalInt.empty()));
        }
    }

    @Test
    void testOutOfSequenceChunk() throws Exception {
        try (var tracker = new SnapshotTracker("test", 2, "leader", fbos, CompressionType.NONE)) {
            tracker.addChunk(1, chunk1, OptionalInt.empty());
            assertThrows(InvalidChunkException.class, () -> tracker.addChunk(3, chunk3, OptionalInt.empty()));
        }
    }

    @Test
    void testInvalidLastChunkHashCode() throws Exception {
        try (var tracker = new SnapshotTracker("test", 2, "leader", fbos, CompressionType.NONE)) {
            tracker.addChunk(1, chunk1, OptionalInt.of(LeaderInstallSnapshotState.INITIAL_LAST_CHUNK_HASH_CODE));
            assertThrows(InvalidChunkException.class, () -> tracker.addChunk(2, chunk2, OptionalInt.of(1)));
        }
    }

    @Test
    void testGetSnapshotBytesWhenNotSealed() throws Exception {
        try (var tracker = new SnapshotTracker("test", 2, "leader", fbos, CompressionType.NONE)) {
            tracker.addChunk(1, chunk1, OptionalInt.empty());
            assertThrows(IllegalStateException.class, tracker::toStreamSource);
        }
    }

    private static byte[] getNextChunk(final ByteString bs, final int offset, int size) {
        int snapshotLength = bs.size();
        int start = offset;
        if (size > snapshotLength) {
            size = snapshotLength;
        } else if (start + size > snapshotLength) {
            size = snapshotLength - start;
        }

        byte[] nextChunk = new byte[size];
        bs.copyTo(nextChunk, start, 0, size);
        return nextChunk;
    }
}
