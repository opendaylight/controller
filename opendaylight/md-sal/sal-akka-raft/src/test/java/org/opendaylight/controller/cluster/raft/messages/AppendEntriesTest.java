/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import static org.junit.Assert.assertEquals;
import java.util.Arrays;
import java.util.Iterator;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext.MockPayload;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.ReplicatedLogImplEntry;

/**
 * Unit tests for AppendEntries.
 *
 * @author Thomas Pantelis
 */
public class AppendEntriesTest {

    @Test
    public void testSerialization() {
        ReplicatedLogEntry entry1 = new ReplicatedLogImplEntry(1, 2, new MockPayload("payload1"));

        ReplicatedLogEntry entry2 = new ReplicatedLogImplEntry(3, 4, new MockPayload("payload2"));

        short payloadVersion = 5;
        AppendEntries expected = new AppendEntries(5L, "node1", 7L, 8L, Arrays.asList(entry1, entry2), 10L,
                -1, payloadVersion);

        AppendEntries cloned = (AppendEntries) SerializationUtils.clone(expected);

        verifyAppendEntries(expected, cloned);
    }

    private static void verifyAppendEntries(AppendEntries expected, AppendEntries actual) {
        assertEquals("getLeaderId", expected.getLeaderId(), actual.getLeaderId());
        assertEquals("getTerm", expected.getTerm(), actual.getTerm());
        assertEquals("getLeaderCommit", expected.getLeaderCommit(), actual.getLeaderCommit());
        assertEquals("getPrevLogIndex", expected.getPrevLogIndex(), actual.getPrevLogIndex());
        assertEquals("getPrevLogTerm", expected.getPrevLogTerm(), actual.getPrevLogTerm());
        assertEquals("getReplicatedToAllIndex", expected.getReplicatedToAllIndex(), actual.getReplicatedToAllIndex());
        assertEquals("getPayloadVersion", expected.getPayloadVersion(), actual.getPayloadVersion());

        assertEquals("getEntries size", expected.getEntries().size(), actual.getEntries().size());
        Iterator<ReplicatedLogEntry> iter = expected.getEntries().iterator();
        for(ReplicatedLogEntry e: actual.getEntries()) {
            verifyReplicatedLogEntry(iter.next(), e);
        }
    }

    private static void verifyReplicatedLogEntry(ReplicatedLogEntry expected, ReplicatedLogEntry actual) {
        assertEquals("getIndex", expected.getIndex(), actual.getIndex());
        assertEquals("getTerm", expected.getTerm(), actual.getTerm());
        assertEquals("getData", expected.getData().toString(), actual.getData().toString());
    }
}
