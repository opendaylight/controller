/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext.MockPayload;
import org.opendaylight.controller.cluster.raft.RaftVersions;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.ReplicatedLogImplEntry;
import org.opendaylight.controller.protobuff.messages.cluster.raft.AppendEntriesMessages;

/**
 * Unit tests for AppendEntries.
 *
 * @author Thomas Pantelis
 */
public class AppendEntriesTest {

    @Test
    public void testSerialization() {
        ReplicatedLogEntry entry1 = new ReplicatedLogImplEntry(1, 2, new MockPayload("one"));

        ReplicatedLogEntry entry2 = new ReplicatedLogImplEntry(3, 4, new MockPayload("two"));

        AppendEntries expected = new AppendEntries(5L, "node1", 7L, 8L, Arrays.asList(entry1, entry2), 10L);

        AppendEntries cloned = (AppendEntries) SerializationUtils.clone(expected);

        verifyAppendEntries(expected, cloned);
    }

    @Test
    public void testToAndFromSerializable() {
        AppendEntries entries = new AppendEntries(5L, "node1", 7L, 8L,
                Collections.<ReplicatedLogEntry>emptyList(), 10L);

        assertSame("toSerializable", entries, entries.toSerializable());
        assertSame("fromSerializable", entries,
                org.opendaylight.controller.cluster.raft.SerializationUtils.fromSerializable(entries));
    }

    @Test
    public void testToAndFromLegacySerializable() {
        AppendEntries entries = new AppendEntries(5L, "node1", 7L, 8L,
                Collections.<ReplicatedLogEntry>emptyList(), 10L);

        Object serializable = entries.toSerializable(RaftVersions.HELIUM_VERSION);
        Assert.assertTrue(serializable instanceof AppendEntriesMessages.AppendEntries);

        AppendEntries entries2 = (AppendEntries)
                org.opendaylight.controller.cluster.raft.SerializationUtils.fromSerializable(serializable);

        verifyAppendEntries(entries, entries2);
    }

    private void verifyAppendEntries(AppendEntries expected, AppendEntries actual) {
        assertEquals("getLeaderId", expected.getLeaderId(), actual.getLeaderId());
        assertEquals("getTerm", expected.getTerm(), actual.getTerm());
        assertEquals("getLeaderCommit", expected.getLeaderCommit(), actual.getLeaderCommit());
        assertEquals("getPrevLogIndex", expected.getPrevLogIndex(), actual.getPrevLogIndex());
        assertEquals("getPrevLogTerm", expected.getPrevLogTerm(), actual.getPrevLogTerm());

        assertEquals("getEntries size", expected.getEntries().size(), actual.getEntries().size());
        Iterator<ReplicatedLogEntry> iter = expected.getEntries().iterator();
        for(ReplicatedLogEntry e: actual.getEntries()) {
            verifyReplicatedLogEntry(iter.next(), e);
        }
    }

    private void verifyReplicatedLogEntry(ReplicatedLogEntry expected, ReplicatedLogEntry actual) {
        assertEquals("getIndex", expected.getIndex(), actual.getIndex());
        assertEquals("getTerm", expected.getTerm(), actual.getTerm());
        assertEquals("getData", expected.getData().toString(), actual.getData().toString());
    }
}
