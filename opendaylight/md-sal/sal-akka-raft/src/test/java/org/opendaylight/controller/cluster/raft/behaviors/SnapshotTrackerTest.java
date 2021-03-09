/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import akka.protobuf.ByteString;
import com.google.common.io.ByteSource;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.controller.cluster.io.FileBackedOutputStream;
import org.opendaylight.controller.cluster.io.FileBackedOutputStreamFactory;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class SnapshotTrackerTest {
    private static final Logger LOG = LoggerFactory.getLogger(SnapshotTrackerTest.class);

    @Mock
    private RaftActorContext mockContext;
    private FileBackedOutputStream fbos;
    private Map<String, String> data;
    private ByteString byteString;
    private byte[] chunk1;
    private byte[] chunk2;
    private byte[] chunk3;

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

        fbos = spy(new FileBackedOutputStream(100000000, "target"));
        FileBackedOutputStreamFactory mockFactory = mock(FileBackedOutputStreamFactory.class);
        doReturn(fbos).when(mockFactory).newInstance();
        doReturn(mockFactory).when(mockContext).getFileBackedOutputStreamFactory();
    }

    @Test
    public void testAddChunks() throws IOException {
        try (SnapshotTracker tracker = new SnapshotTracker(LOG, 3, "leader", mockContext)) {
            tracker.addChunk(1, chunk1, OptionalInt.of(LeaderInstallSnapshotState.INITIAL_LAST_CHUNK_HASH_CODE));
            tracker.addChunk(2, chunk2, OptionalInt.of(Arrays.hashCode(chunk1)));
            tracker.addChunk(3, chunk3, OptionalInt.of(Arrays.hashCode(chunk2)));

            ByteSource snapshotBytes = tracker.getSnapshotBytes();
            assertEquals("Deserialized", data, SerializationUtils.deserialize(snapshotBytes.read()));
        }

        verify(fbos).cleanup();
    }

    @Test(expected = SnapshotTracker.InvalidChunkException.class)
    public void testAddChunkWhenAlreadySealed() throws IOException {
        try (SnapshotTracker tracker = new SnapshotTracker(LOG, 2, "leader", mockContext)) {
            tracker.addChunk(1, chunk1, OptionalInt.empty());
            tracker.addChunk(2, chunk2, OptionalInt.empty());
            tracker.addChunk(3, chunk3, OptionalInt.empty());
        }
    }

    @Test(expected = SnapshotTracker.InvalidChunkException.class)
    public void testInvalidFirstChunkIndex() throws IOException {
        try (SnapshotTracker tracker = new SnapshotTracker(LOG, 2, "leader", mockContext)) {
            tracker.addChunk(LeaderInstallSnapshotState.FIRST_CHUNK_INDEX - 1, chunk1, OptionalInt.empty());
        }
    }

    @Test(expected = SnapshotTracker.InvalidChunkException.class)
    public void testOutOfSequenceChunk() throws IOException {
        try (SnapshotTracker tracker = new SnapshotTracker(LOG, 2, "leader", mockContext)) {
            tracker.addChunk(1, chunk1, OptionalInt.empty());
            tracker.addChunk(3, chunk3, OptionalInt.empty());
        }
    }

    @Test(expected = SnapshotTracker.InvalidChunkException.class)
    public void testInvalidLastChunkHashCode() throws IOException {
        try (SnapshotTracker tracker = new SnapshotTracker(LOG, 2, "leader", mockContext)) {
            tracker.addChunk(1, chunk1, OptionalInt.of(LeaderInstallSnapshotState.INITIAL_LAST_CHUNK_HASH_CODE));
            tracker.addChunk(2, chunk2, OptionalInt.of(1));
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testGetSnapshotBytesWhenNotSealed() throws IOException {
        try (SnapshotTracker tracker = new SnapshotTracker(LOG, 2, "leader", mockContext)) {
            tracker.addChunk(1, chunk1, OptionalInt.empty());
            tracker.getSnapshotBytes();
        }
    }

    private static byte[] getNextChunk(final ByteString bs, final int offset, int size) {
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
