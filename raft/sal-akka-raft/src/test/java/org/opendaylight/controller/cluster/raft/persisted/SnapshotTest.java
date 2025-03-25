/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.Test;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext.MockPayload;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.raft.api.TermInfo;

/**
 * Unit tests for Snapshot.
 *
 * @author Thomas Pantelis
 */
class SnapshotTest {
    @Test
    void testSerialization() {
        testSerialization(new byte[]{1, 2, 3, 4, 5, 6, 7}, List.of(
                new SimpleReplicatedLogEntry(6, 2, new MockPayload("payload"))), 491);
        testSerialization(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9}, List.of(), 345);
    }

    private static void testSerialization(final byte[] state, final List<ReplicatedLogEntry> unapplied,
            final int expectedSize) {
        long lastIndex = 6;
        long lastTerm = 2;
        long lastAppliedIndex = 5;
        long lastAppliedTerm = 1;
        long electionTerm = 3;
        String electionVotedFor = "member-1";
        final var serverConfig = new ClusterConfig(new ServerInfo("1", true), new ServerInfo("2", false));

        final var expected = Snapshot.create(ByteState.of(state), unapplied, lastIndex, lastTerm, lastAppliedIndex,
                lastAppliedTerm, new TermInfo(electionTerm, electionVotedFor), serverConfig);
        final var bytes = SerializationUtils.serialize(expected);
        assertEquals(expectedSize, bytes.length);
        final var cloned = (Snapshot) SerializationUtils.deserialize(bytes);

        assertEquals(expected.getLastIndex(), cloned.getLastIndex());
        assertEquals(expected.getLastTerm(), cloned.getLastTerm());
        assertEquals(expected.getLastAppliedIndex(), cloned.getLastAppliedIndex());
        assertEquals(expected.getLastAppliedTerm(), cloned.getLastAppliedTerm());
        assertEquals(expected.getUnAppliedEntries(), cloned.getUnAppliedEntries());
        assertEquals(expected.termInfo(), cloned.termInfo());
        assertEquals(expected.getState(), cloned.getState());
        assertEquals(expected.getServerConfiguration().serverInfo(), cloned.getServerConfiguration().serverInfo());
    }
}
