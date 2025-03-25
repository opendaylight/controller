/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext.MockPayload;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;

class AbstractReplicatedLogTest {
    private MockReplicatedLog replicatedLogImpl;

    @BeforeEach
    void beforeEach() {
        replicatedLogImpl = new MockReplicatedLog();
        // create a set of initial entries in the in-memory log
        replicatedLogImpl.append(new SimpleReplicatedLogEntry(0, 1, new MockPayload("A")));
        replicatedLogImpl.append(new SimpleReplicatedLogEntry(1, 1, new MockPayload("B")));
        replicatedLogImpl.append(new SimpleReplicatedLogEntry(2, 1, new MockPayload("C")));
        replicatedLogImpl.append(new SimpleReplicatedLogEntry(3, 2, new MockPayload("D")));
    }

    @Test
    void testEmptyLog() {
        replicatedLogImpl = new MockReplicatedLog();

        assertEquals(0, replicatedLogImpl.size());
        assertEquals(0, replicatedLogImpl.dataSize());
        assertEquals(-1, replicatedLogImpl.getSnapshotIndex());
        assertEquals(-1, replicatedLogImpl.getSnapshotTerm());
        assertEquals(-1, replicatedLogImpl.lastIndex());
        assertEquals(-1, replicatedLogImpl.lastTerm());
        assertFalse(replicatedLogImpl.isPresent(0));
        assertFalse(replicatedLogImpl.isInSnapshot(0));
        assertNull(replicatedLogImpl.get(0));
        assertNull(replicatedLogImpl.last());

        assertEquals(List.of(), replicatedLogImpl.getFrom(0, 1, ReplicatedLog.NO_MAX_SIZE));

        assertEquals(-1, replicatedLogImpl.removeFrom(1));

        replicatedLogImpl.setSnapshotIndex(2);
        replicatedLogImpl.setSnapshotTerm(1);

        assertEquals(2, replicatedLogImpl.getSnapshotIndex());
        assertEquals(1, replicatedLogImpl.getSnapshotTerm());
        assertEquals(2, replicatedLogImpl.lastIndex());
        assertEquals(1, replicatedLogImpl.lastTerm());
    }

    @Test
    void testIndexOperations() {
        // check if the values returned are correct, with snapshotIndex = -1
        assertEquals("B", replicatedLogImpl.get(1).getData().toString());
        assertEquals("D", replicatedLogImpl.last().getData().toString());
        assertEquals(3, replicatedLogImpl.lastIndex());
        assertEquals(2, replicatedLogImpl.lastTerm());
        assertEquals(2, replicatedLogImpl.getFrom(2).size());
        assertEquals(4, replicatedLogImpl.size());
        assertTrue(replicatedLogImpl.isPresent(2));
        assertFalse(replicatedLogImpl.isPresent(4));
        assertFalse(replicatedLogImpl.isInSnapshot(2));

        // now create a snapshot of 3 entries, with 1 unapplied entry left in the log
        // It removes the entries which have made it to snapshot
        // and updates the snapshot index and term
        takeSnapshot(3);

        // check the values after the snapshot.
        // each index value passed in the test is the logical index (log entry index)
        // which gets mapped to the list's physical index
        assertEquals("D", replicatedLogImpl.get(3).getData().toString());
        assertEquals("D", replicatedLogImpl.last().getData().toString());
        assertNull(replicatedLogImpl.get(1));
        assertEquals(3, replicatedLogImpl.lastIndex());
        assertEquals(2, replicatedLogImpl.lastTerm());
        assertEquals(0, replicatedLogImpl.getFrom(2).size());
        assertEquals(1, replicatedLogImpl.size());
        assertFalse(replicatedLogImpl.isPresent(2));
        assertTrue(replicatedLogImpl.isPresent(3));
        assertFalse(replicatedLogImpl.isPresent(4));
        assertTrue(replicatedLogImpl.isInSnapshot(2));

        // append few more entries
        replicatedLogImpl.append(new SimpleReplicatedLogEntry(4, 2, new MockPayload("E")));
        replicatedLogImpl.append(new SimpleReplicatedLogEntry(5, 2, new MockPayload("F")));
        replicatedLogImpl.append(new SimpleReplicatedLogEntry(6, 3, new MockPayload("G")));
        replicatedLogImpl.append(new SimpleReplicatedLogEntry(7, 3, new MockPayload("H")));

        // check their values as well
        assertEquals(5, replicatedLogImpl.size());
        assertEquals("D", replicatedLogImpl.get(3).getData().toString());
        assertEquals("E", replicatedLogImpl.get(4).getData().toString());
        assertEquals("H", replicatedLogImpl.last().getData().toString());
        assertEquals(3, replicatedLogImpl.lastTerm());
        assertEquals(7, replicatedLogImpl.lastIndex());
        assertTrue(replicatedLogImpl.isPresent(7));
        assertFalse(replicatedLogImpl.isInSnapshot(7));
        assertEquals(1, replicatedLogImpl.getFrom(7).size());
        assertEquals(2, replicatedLogImpl.getFrom(6).size());

        // take a second snapshot with 5 entries with 0 unapplied entries left in the log
        takeSnapshot(5);

        assertEquals(0, replicatedLogImpl.size());
        assertNull(replicatedLogImpl.last());
        assertNull(replicatedLogImpl.get(7));
        assertNull(replicatedLogImpl.get(1));
        assertFalse(replicatedLogImpl.isPresent(7));
        assertTrue(replicatedLogImpl.isInSnapshot(7));
        assertEquals(0, replicatedLogImpl.getFrom(7).size());
        assertEquals(0, replicatedLogImpl.getFrom(6).size());
    }

    @Test
    void testGetFromWithMax() {
        var from = replicatedLogImpl.getFrom(0, 1, ReplicatedLog.NO_MAX_SIZE);
        assertEquals(1, from.size());
        assertEquals("A", from.get(0).getData().toString());

        from = replicatedLogImpl.getFrom(0, 20, ReplicatedLog.NO_MAX_SIZE);
        assertEquals(4, from.size());
        assertEquals("A", from.get(0).getData().toString());
        assertEquals("B", from.get(1).getData().toString());
        assertEquals("C", from.get(2).getData().toString());
        assertEquals("D", from.get(3).getData().toString());

        // Pre-calculate sizing information for use with capping
        final int sizeB = from.get(1).serializedSize();
        final int sizeC = from.get(2).serializedSize();
        final int sizeD = from.get(3).serializedSize();

        from = replicatedLogImpl.getFrom(1, 2, ReplicatedLog.NO_MAX_SIZE);
        assertEquals(2, from.size());
        assertEquals("B", from.get(0).getData().toString());
        assertEquals("C", from.get(1).getData().toString());

        from = replicatedLogImpl.getFrom(1, 3, sizeB + sizeC);
        assertEquals(2, from.size());
        assertEquals("B", from.get(0).getData().toString());
        assertEquals("C", from.get(1).getData().toString());

        from = replicatedLogImpl.getFrom(1, 3, sizeB + sizeC + sizeD);
        assertEquals(3, from.size());
        assertEquals("B", from.get(0).getData().toString());
        assertEquals("C", from.get(1).getData().toString());
        assertEquals("D", from.get(2).getData().toString());

        from = replicatedLogImpl.getFrom(1, 2, sizeB + sizeC + sizeD);
        assertEquals(2, from.size());
        assertEquals("B", from.get(0).getData().toString());
        assertEquals("C", from.get(1).getData().toString());

        replicatedLogImpl.append(new SimpleReplicatedLogEntry(4, 2, new MockPayload("12345")));
        from = replicatedLogImpl.getFrom(4, 2, 2);
        assertEquals(1, from.size());
        assertEquals("12345", from.get(0).getData().toString());
    }

    @Test
    void testSnapshotPreCommit() {
        //add 4 more entries
        replicatedLogImpl.append(new SimpleReplicatedLogEntry(4, 2, new MockPayload("E")));
        replicatedLogImpl.append(new SimpleReplicatedLogEntry(5, 2, new MockPayload("F")));
        replicatedLogImpl.append(new SimpleReplicatedLogEntry(6, 3, new MockPayload("G")));
        replicatedLogImpl.append(new SimpleReplicatedLogEntry(7, 3, new MockPayload("H")));

        //sending negative values should not cause any changes
        replicatedLogImpl.snapshotPreCommit(-1, -1);
        assertEquals(8, replicatedLogImpl.size());
        assertEquals(-1, replicatedLogImpl.getSnapshotIndex());
        assertEquals(-1, replicatedLogImpl.getSnapshotTerm());

        replicatedLogImpl.snapshotPreCommit(4, 2);
        assertEquals(3, replicatedLogImpl.size());
        assertEquals(4, replicatedLogImpl.getSnapshotIndex());
        assertEquals(2, replicatedLogImpl.getSnapshotTerm());

        replicatedLogImpl.snapshotPreCommit(6, 3);
        assertEquals(1, replicatedLogImpl.size());
        assertEquals(6, replicatedLogImpl.getSnapshotIndex());
        assertEquals(3, replicatedLogImpl.getSnapshotTerm());

        replicatedLogImpl.snapshotPreCommit(7, 3);
        assertEquals(0, replicatedLogImpl.size());
        assertEquals(7, replicatedLogImpl.getSnapshotIndex());
        assertEquals(3, replicatedLogImpl.getSnapshotTerm());

        //running it again on an empty list should not throw exception
        replicatedLogImpl.snapshotPreCommit(7, 3);
        assertEquals(0, replicatedLogImpl.size());
        assertEquals(7, replicatedLogImpl.getSnapshotIndex());
        assertEquals(3, replicatedLogImpl.getSnapshotTerm());
    }

    @Test
    void testSnapshotCommit() {
        replicatedLogImpl.snapshotPreCommit(1, 1);
        replicatedLogImpl.snapshotCommit();

        assertEquals(2, replicatedLogImpl.size());
        assertEquals(2, replicatedLogImpl.dataSize());
        assertEquals(1, replicatedLogImpl.getSnapshotIndex());
        assertEquals(1, replicatedLogImpl.getSnapshotTerm());
        assertEquals(3, replicatedLogImpl.lastIndex());
        assertEquals(2, replicatedLogImpl.lastTerm());

        assertNull(replicatedLogImpl.get(0));
        assertNull(replicatedLogImpl.get(1));
        assertNotNull(replicatedLogImpl.get(2));
        assertNotNull(replicatedLogImpl.get(3));
    }

    @Test
    void testSnapshotRollback() {
        replicatedLogImpl.snapshotPreCommit(1, 1);

        assertEquals(2, replicatedLogImpl.size());
        assertEquals(1, replicatedLogImpl.getSnapshotIndex());
        assertEquals(1, replicatedLogImpl.getSnapshotTerm());

        replicatedLogImpl.snapshotRollback();

        assertEquals(4, replicatedLogImpl.size());
        assertEquals(4, replicatedLogImpl.dataSize());
        assertEquals(-1, replicatedLogImpl.getSnapshotIndex());
        assertEquals(-1, replicatedLogImpl.getSnapshotTerm());
        assertNotNull(replicatedLogImpl.get(0));
        assertNotNull(replicatedLogImpl.get(3));
    }

    @Test
    void testIsPresent() {
        assertTrue(replicatedLogImpl.isPresent(0));
        assertTrue(replicatedLogImpl.isPresent(1));
        assertTrue(replicatedLogImpl.isPresent(2));
        assertTrue(replicatedLogImpl.isPresent(3));

        replicatedLogImpl.append(new SimpleReplicatedLogEntry(4, 2, new MockPayload("D")));
        replicatedLogImpl.snapshotPreCommit(3, 2); //snapshot on 3
        replicatedLogImpl.snapshotCommit();

        assertFalse(replicatedLogImpl.isPresent(0));
        assertFalse(replicatedLogImpl.isPresent(1));
        assertFalse(replicatedLogImpl.isPresent(2));
        assertFalse(replicatedLogImpl.isPresent(3));
        assertTrue(replicatedLogImpl.isPresent(4));

        replicatedLogImpl.snapshotPreCommit(4, 2); //snapshot on 4
        replicatedLogImpl.snapshotCommit();
        assertFalse(replicatedLogImpl.isPresent(4));

        replicatedLogImpl.append(new SimpleReplicatedLogEntry(5, 2, new MockPayload("D")));
        assertTrue(replicatedLogImpl.isPresent(5));
    }

    @Test
    void testRemoveFrom() {
        replicatedLogImpl.append(new SimpleReplicatedLogEntry(4, 2, new MockPayload("E", 2)));
        replicatedLogImpl.append(new SimpleReplicatedLogEntry(5, 2, new MockPayload("F", 3)));

        assertEquals(9, replicatedLogImpl.dataSize());

        long adjusted = replicatedLogImpl.removeFrom(4);
        assertEquals(4, adjusted);
        assertEquals(4, replicatedLogImpl.size());
        assertEquals(4, replicatedLogImpl.dataSize());

        takeSnapshot(1);

        adjusted = replicatedLogImpl.removeFrom(2);
        assertEquals(1, adjusted);
        assertEquals(1, replicatedLogImpl.size());
        assertEquals(1, replicatedLogImpl.dataSize());

        assertEquals(-1, replicatedLogImpl.removeFrom(0));
        assertEquals(-1, replicatedLogImpl.removeFrom(100));
    }

    // create a snapshot for test
    private Map<Long, String> takeSnapshot(final int numEntries) {
        final var map = new HashMap<Long, String>(numEntries);

        long lastIndex = 0;
        long lastTerm = 0;
        for (int i = 0; i < numEntries; i++) {
            final var entry = replicatedLogImpl.getAtPhysicalIndex(i);
            map.put(entry.index(), entry.getData().toString());
            lastIndex = entry.index();
            lastTerm = entry.term();
        }

        replicatedLogImpl.snapshotPreCommit(lastIndex, lastTerm);
        replicatedLogImpl.snapshotCommit();

        return map;
    }
}
