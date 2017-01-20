/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.behaviors;

import static org.junit.Assert.assertEquals;

import com.google.common.base.Optional;
import com.google.common.io.ByteSource;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SnapshotTrackerTest {

    Logger logger = LoggerFactory.getLogger(getClass());

    Map<String, String> data;
    ByteString byteString;
    byte[] chunk1;
    byte[] chunk2;
    byte[] chunk3;

    @Before
    public void setup() {
        data = new HashMap<>();
        data.put("key1", "value1");
        data.put("key2", "value2");
        data.put("key3", "value3");

        byteString = ByteString.copyFrom(SerializationUtils.serialize((Serializable) data));
        chunk1 = getNextChunk(byteString, 0, 10);
        chunk2 = getNextChunk(byteString, 10, 10);
        chunk3 = getNextChunk(byteString, 20, byteString.size());
    }

    @Test
    public void testAddChunks() throws IOException {
        SnapshotTracker tracker = new SnapshotTracker(logger, 3, "leader");

        tracker.addChunk(1, chunk1, Optional.of(LeaderInstallSnapshotState.INITIAL_LAST_CHUNK_HASH_CODE));
        tracker.addChunk(2, chunk2, Optional.of(Arrays.hashCode(chunk1)));
        tracker.addChunk(3, chunk3, Optional.of(Arrays.hashCode(chunk2)));

        ByteSource snapshotBytes = tracker.getSnapshotBytes();
        assertEquals("Deserialized", data, SerializationUtils.deserialize(snapshotBytes.read()));

        tracker.close();
    }

    @Test(expected = SnapshotTracker.InvalidChunkException.class)
    public void testAddChunkWhenAlreadySealed() throws IOException {
        try (SnapshotTracker tracker = new SnapshotTracker(logger, 2, "leader")) {
            tracker.addChunk(1, chunk1, Optional.<Integer>absent());
            tracker.addChunk(2, chunk2, Optional.<Integer>absent());
            tracker.addChunk(3, chunk3, Optional.<Integer>absent());
        }
    }

    @Test(expected = SnapshotTracker.InvalidChunkException.class)
    public void testInvalidFirstChunkIndex() throws IOException {
        try (SnapshotTracker tracker = new SnapshotTracker(logger, 2, "leader")) {
            tracker.addChunk(LeaderInstallSnapshotState.FIRST_CHUNK_INDEX - 1, chunk1, Optional.<Integer>absent());
        }
    }

    @Test(expected = SnapshotTracker.InvalidChunkException.class)
    public void testOutOfSequenceChunk() throws IOException {
        try (SnapshotTracker tracker = new SnapshotTracker(logger, 2, "leader")) {
            tracker.addChunk(1, chunk1, Optional.<Integer>absent());
            tracker.addChunk(3, chunk3, Optional.<Integer>absent());
        }
    }

    @Test(expected = SnapshotTracker.InvalidChunkException.class)
    public void testInvalidLastChunkHashCode() throws IOException {
        try (SnapshotTracker tracker = new SnapshotTracker(logger, 2, "leader")) {
            tracker.addChunk(1, chunk1, Optional.of(LeaderInstallSnapshotState.INITIAL_LAST_CHUNK_HASH_CODE));
            tracker.addChunk(2, chunk2, Optional.of(1));
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testGetSnapshotBytesWhenNotSealed() throws IOException {
        try (SnapshotTracker tracker = new SnapshotTracker(logger, 2, "leader")) {
            tracker.addChunk(1, chunk1, Optional.<Integer>absent());
            tracker.getSnapshotBytes();
        }
    }

    private byte[] getNextChunk(ByteString bs, int offset, int size) {
        int snapshotLength = bs.size();
        int start = offset;
        if (size > snapshotLength) {
            size = snapshotLength;
        } else {
            if (start + size > snapshotLength) {
                size = snapshotLength - start;
            }
        }

        byte[] nextChunk = new byte[size];
        bs.copyTo(nextChunk, start, 0, size);
        return nextChunk;
    }
}
