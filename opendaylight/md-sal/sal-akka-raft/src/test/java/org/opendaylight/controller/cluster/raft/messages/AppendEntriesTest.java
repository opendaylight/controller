/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.Test;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext.MockPayload;
import org.opendaylight.controller.cluster.raft.RaftVersions;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;

/**
 * Unit tests for AppendEntries.
 *
 * @author Thomas Pantelis
 */
class AppendEntriesTest {
    @Test
    void testSerialization() {
        var entry1 = new SimpleReplicatedLogEntry(1, 2, new MockPayload("payload1"));

        var entry2 = new SimpleReplicatedLogEntry(3, 4, new MockPayload("payload2"));

        short payloadVersion = 5;

        // Without leader address

        var expected = new AppendEntries(5L, "node1", 7L, 8L, List.of(entry1, entry2), 10L, -1, payloadVersion,
            RaftVersions.CURRENT_VERSION, null);

        var bytes = SerializationUtils.serialize(expected);
        assertEquals(285, bytes.length);
        var cloned = assertInstanceOf(AppendEntries.class, SerializationUtils.deserialize(bytes));

        assertAppendEntries(expected, cloned, RaftVersions.CURRENT_VERSION);

        // With leader address

        expected = new AppendEntries(5L, "node1", 7L, 8L, List.of(entry1, entry2), 10L, -1, payloadVersion,
            RaftVersions.CURRENT_VERSION, "leader address");

        bytes = SerializationUtils.serialize(expected);
        assertEquals(301, bytes.length);
        cloned = assertInstanceOf(AppendEntries.class, SerializationUtils.deserialize(bytes));

        assertAppendEntries(expected, cloned, RaftVersions.CURRENT_VERSION);
    }

    private static void assertAppendEntries(final AppendEntries expected, final AppendEntries actual,
            final short recipientRaftVersion) {
        assertEquals(expected.getLeaderId(), actual.getLeaderId());
        assertEquals(expected.getTerm(), actual.getTerm());
        assertEquals(expected.getLeaderCommit(), actual.getLeaderCommit());
        assertEquals(expected.getPrevLogIndex(), actual.getPrevLogIndex());
        assertEquals(expected.getPrevLogTerm(), actual.getPrevLogTerm());
        assertEquals(expected.getReplicatedToAllIndex(), actual.getReplicatedToAllIndex());
        assertEquals(expected.getPayloadVersion(), actual.getPayloadVersion());

        assertEquals(expected.getEntries().size(), actual.getEntries().size());
        var iter = expected.getEntries().iterator();
        for (var entry : actual.getEntries()) {
            assertReplicatedLogEntry(iter.next(), entry);
        }

        assertEquals(expected.leaderAddress(), actual.leaderAddress());
        assertEquals(RaftVersions.CURRENT_VERSION, actual.getLeaderRaftVersion());
    }

    private static void assertReplicatedLogEntry(final ReplicatedLogEntry expected, final ReplicatedLogEntry actual) {
        assertEquals(expected.index(), actual.index());
        assertEquals(expected.term(), actual.term());
        assertEquals(expected.getData().toString(), actual.getData().toString());
    }
}
