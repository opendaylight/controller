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
import com.google.protobuf.ByteString;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
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
    public void setup(){
        data = new HashMap<>();
        data.put("key1", "value1");
        data.put("key2", "value2");
        data.put("key3", "value3");

        byteString = toByteString(data);
        chunk1 = getNextChunk(byteString, 0, 10);
        chunk2 = getNextChunk(byteString, 10, 10);
        chunk3 = getNextChunk(byteString, 20, byteString.size());
    }

    @Test
    public void testAddChunk() throws SnapshotTracker.InvalidChunkException {
        SnapshotTracker tracker1 = new SnapshotTracker(logger, 5);

        tracker1.addChunk(1, chunk1, Optional.<Integer>absent());
        tracker1.addChunk(2, chunk2, Optional.<Integer>absent());
        tracker1.addChunk(3, chunk3, Optional.<Integer>absent());

        // Verify that an InvalidChunkException is thrown when we try to add a chunk to a sealed tracker
        SnapshotTracker tracker2 = new SnapshotTracker(logger, 2);

        tracker2.addChunk(1, chunk1, Optional.<Integer>absent());
        tracker2.addChunk(2, chunk2, Optional.<Integer>absent());

        try {
            tracker2.addChunk(3, chunk3, Optional.<Integer>absent());
            Assert.fail();
        } catch(SnapshotTracker.InvalidChunkException e){
            e.getMessage().startsWith("Invalid chunk");
        }

        // The first chunk's index must at least be FIRST_CHUNK_INDEX
        SnapshotTracker tracker3 = new SnapshotTracker(logger, 2);

        try {
            tracker3.addChunk(AbstractLeader.FIRST_CHUNK_INDEX - 1, chunk1, Optional.<Integer>absent());
            Assert.fail();
        } catch(SnapshotTracker.InvalidChunkException e){

        }

        // Out of sequence chunk indexes won't work
        SnapshotTracker tracker4 = new SnapshotTracker(logger, 2);

        tracker4.addChunk(AbstractLeader.FIRST_CHUNK_INDEX, chunk1, Optional.<Integer>absent());

        try {
            tracker4.addChunk(AbstractLeader.FIRST_CHUNK_INDEX+2, chunk2, Optional.<Integer>absent());
            Assert.fail();
        } catch(SnapshotTracker.InvalidChunkException e){

        }

        // No exceptions will be thrown when invalid chunk is added with the right sequence
        // If the lastChunkHashCode is missing
        SnapshotTracker tracker5 = new SnapshotTracker(logger, 2);

        tracker5.addChunk(AbstractLeader.FIRST_CHUNK_INDEX, chunk1, Optional.<Integer>absent());
        // Look I can add the same chunk again
        tracker5.addChunk(AbstractLeader.FIRST_CHUNK_INDEX + 1, chunk1, Optional.<Integer>absent());

        // An exception will be thrown when an invalid chunk is addedd with the right sequence
        // when the lastChunkHashCode is present
        SnapshotTracker tracker6 = new SnapshotTracker(logger, 2);

        tracker6.addChunk(AbstractLeader.FIRST_CHUNK_INDEX, chunk1, Optional.of(-1));

        try {
            // Here we add a second chunk and tell addChunk that the previous chunk had a hash code 777
            tracker6.addChunk(AbstractLeader.FIRST_CHUNK_INDEX + 1, chunk2, Optional.of(777));
            Assert.fail();
        }catch(SnapshotTracker.InvalidChunkException e){

        }

    }

    @Test
    public void testGetSnapShot() throws SnapshotTracker.InvalidChunkException {

        // Trying to get a snapshot before all chunks have been received will throw an exception
        SnapshotTracker tracker1 = new SnapshotTracker(logger, 5);

        tracker1.addChunk(1, chunk1, Optional.<Integer>absent());
        try {
            tracker1.getSnapshot();
            Assert.fail();
        } catch(IllegalStateException e){

        }

        SnapshotTracker tracker2 = new SnapshotTracker(logger, 3);

        tracker2.addChunk(1, chunk1, Optional.of(AbstractLeader.INITIAL_LAST_CHUNK_HASH_CODE));
        tracker2.addChunk(2, chunk2, Optional.of(Arrays.hashCode(chunk1)));
        tracker2.addChunk(3, chunk3, Optional.of(Arrays.hashCode(chunk2)));

        byte[] snapshot = tracker2.getSnapshot();

        assertEquals(byteString, ByteString.copyFrom(snapshot));
    }

    @Test
    public void testGetCollectedChunks() throws SnapshotTracker.InvalidChunkException {
        SnapshotTracker tracker1 = new SnapshotTracker(logger, 5);

        ByteString chunks = ByteString.copyFrom(chunk1).concat(ByteString.copyFrom(chunk2));

        tracker1.addChunk(1, chunk1, Optional.of(AbstractLeader.INITIAL_LAST_CHUNK_HASH_CODE));
        tracker1.addChunk(2, chunk2, Optional.of(Arrays.hashCode(chunk1)));

        assertEquals(chunks, tracker1.getCollectedChunks());
    }

    public byte[] getNextChunk (ByteString bs, int offset, int size){
        int snapshotLength = bs.size();
        int start = offset;
        if (size > snapshotLength) {
            size = snapshotLength;
        } else {
            if ((start + size) > snapshotLength) {
                size = snapshotLength - start;
            }
        }

        byte[] nextChunk = new byte[size];
        bs.copyTo(nextChunk, start, 0, size);
        return nextChunk;
    }

    private static ByteString toByteString(Map<String, String> state) {
        ByteArrayOutputStream b = null;
        ObjectOutputStream o = null;
        try {
            try {
                b = new ByteArrayOutputStream();
                o = new ObjectOutputStream(b);
                o.writeObject(state);
                byte[] snapshotBytes = b.toByteArray();
                return ByteString.copyFrom(snapshotBytes);
            } finally {
                if (o != null) {
                    o.flush();
                    o.close();
                }
                if (b != null) {
                    b.close();
                }
            }
        } catch (IOException e) {
            org.junit.Assert.fail("IOException in converting Hashmap to Bytestring:" + e);
        }
        return null;
    }


}