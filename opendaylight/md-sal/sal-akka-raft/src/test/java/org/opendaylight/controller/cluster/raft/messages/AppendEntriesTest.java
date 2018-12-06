/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.Iterator;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext.MockPayload;
import org.opendaylight.controller.cluster.raft.RaftVersions;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;

/**
 * Unit tests for AppendEntries.
 *
 * @author Thomas Pantelis
 */
public class AppendEntriesTest {

    @Test
    public void testSerialization() {
        ReplicatedLogEntry entry1 = new SimpleReplicatedLogEntry(1, 2, new MockPayload("payload1"));

        ReplicatedLogEntry entry2 = new SimpleReplicatedLogEntry(3, 4, new MockPayload("payload2"));

        short payloadVersion = 5;

        // Without leader address

        AppendEntries expected = new AppendEntries(5L, "node1", 7L, 8L, Arrays.asList(entry1, entry2), 10L,
                -1, payloadVersion, RaftVersions.CURRENT_VERSION, null);

        AppendEntries cloned = (AppendEntries) SerializationUtils.clone(expected);

        verifyAppendEntries(expected, cloned, RaftVersions.CURRENT_VERSION);

        // With leader address

        expected = new AppendEntries(5L, "node1", 7L, 8L, Arrays.asList(entry1, entry2), 10L,
                -1, payloadVersion, RaftVersions.CURRENT_VERSION, "leader address");

        cloned = (AppendEntries) SerializationUtils.clone(expected);

        verifyAppendEntries(expected, cloned, RaftVersions.CURRENT_VERSION);
    }

    @Test
    @Deprecated
    public void testPreFluorineSerialization() {
        ReplicatedLogEntry entry1 = new SimpleReplicatedLogEntry(1, 2, new MockPayload("payload1"));

        ReplicatedLogEntry entry2 = new SimpleReplicatedLogEntry(3, 4, new MockPayload("payload2"));

        short payloadVersion = 5;

        AppendEntries expected = new AppendEntries(5L, "node1", 7L, 8L, Arrays.asList(entry1, entry2), 10L,
                -1, payloadVersion, RaftVersions.BORON_VERSION, "leader address");

        AppendEntries cloned = (AppendEntries) SerializationUtils.clone(expected);

        verifyAppendEntries(expected, cloned, RaftVersions.BORON_VERSION);
    }

    private static void verifyAppendEntries(AppendEntries expected, AppendEntries actual, short recipientRaftVersion) {
        assertEquals("getLeaderId", expected.getLeaderId(), actual.getLeaderId());
        assertEquals("getTerm", expected.getTerm(), actual.getTerm());
        assertEquals("getLeaderCommit", expected.getLeaderCommit(), actual.getLeaderCommit());
        assertEquals("getPrevLogIndex", expected.getPrevLogIndex(), actual.getPrevLogIndex());
        assertEquals("getPrevLogTerm", expected.getPrevLogTerm(), actual.getPrevLogTerm());
        assertEquals("getReplicatedToAllIndex", expected.getReplicatedToAllIndex(), actual.getReplicatedToAllIndex());
        assertEquals("getPayloadVersion", expected.getPayloadVersion(), actual.getPayloadVersion());

        assertEquals("getEntries size", expected.getEntries().size(), actual.getEntries().size());
        Iterator<ReplicatedLogEntry> iter = expected.getEntries().iterator();
        for (ReplicatedLogEntry e: actual.getEntries()) {
            verifyReplicatedLogEntry(iter.next(), e);
        }

        if (recipientRaftVersion >= RaftVersions.FLUORINE_VERSION) {
            assertEquals("getLeaderAddress", expected.getLeaderAddress(), actual.getLeaderAddress());
            assertEquals("getLeaderRaftVersion", RaftVersions.CURRENT_VERSION, actual.getLeaderRaftVersion());
        } else {
            assertFalse(actual.getLeaderAddress().isPresent());
            assertEquals("getLeaderRaftVersion", RaftVersions.BORON_VERSION, actual.getLeaderRaftVersion());
        }
    }

    private static void verifyReplicatedLogEntry(ReplicatedLogEntry expected, ReplicatedLogEntry actual) {
        assertEquals("getIndex", expected.getIndex(), actual.getIndex());
        assertEquals("getTerm", expected.getTerm(), actual.getTerm());
        assertEquals("getData", expected.getData().toString(), actual.getData().toString());
    }
}
