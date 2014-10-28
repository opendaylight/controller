/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import static org.junit.Assert.assertEquals;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.ReplicatedLogImplEntry;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyLogEntries;
import org.opendaylight.controller.cluster.raft.base.messages.ElectionTimeout;

/**
 *  Tests serialization of message classes.
 *
 * @author Thomas Pantelis
 */
public class SerializationTest {

    @Test
    public void testAppendEntries() {
        List<ReplicatedLogEntry> entries = Arrays.<ReplicatedLogEntry>asList(
                new ReplicatedLogImplEntry(1, 2, new MockRaftActorContext.MockPayload("1")),
                new ReplicatedLogImplEntry(2, 3, new MockRaftActorContext.MockPayload("2")));
        AppendEntries expected = new AppendEntries(2, "leader", 3, 1, entries , 4);
        AppendEntries actual = (AppendEntries) SerializationUtils.clone(expected);
        assertEquals("getLeaderId", expected.getLeaderId(), actual.getLeaderId());
        assertEquals("getLeaderCommit", expected.getLeaderCommit(), actual.getLeaderCommit());
        assertEquals("getPrevLogIndex", expected.getPrevLogIndex(), actual.getPrevLogIndex());
        assertEquals("getPrevLogTerm", expected.getPrevLogTerm(), actual.getPrevLogTerm());
        assertEquals("getTerm", expected.getTerm(), actual.getTerm());

        List<ReplicatedLogEntry> expectedEntries = expected.getEntries();
        List<ReplicatedLogEntry> actualEntries = actual.getEntries();
        assertEquals("getEntries size", expectedEntries.size(), actualEntries.size());
        for(int i = 0; i < expectedEntries.size(); i++) {
            ReplicatedLogEntry expEntry = expectedEntries.get(i);
            ReplicatedLogEntry actualEntry = actualEntries.get(i);
            assertEquals("Entry getTerm", expEntry.getTerm(), actualEntry.getTerm());
            assertEquals("Entry getIndex", expEntry.getIndex(), actualEntry.getIndex());
            assertEquals("Entry getData", expEntry.getData(), actualEntry.getData());
        }
    }

    @Test
    public void testAppendEntriesReply() {
        AppendEntriesReply expected = new AppendEntriesReply("follower", 1, true, 2, 3);
        AppendEntriesReply actual = (AppendEntriesReply) SerializationUtils.clone(expected);
        assertEquals("getFollowerId", expected.getFollowerId(), actual.getFollowerId());
        assertEquals("getLogLastIndex", expected.getLogLastIndex(), actual.getLogLastIndex());
        assertEquals("getLogLastTerm", expected.getLogLastTerm(), actual.getLogLastTerm());
        assertEquals("getTerm", expected.getTerm(), actual.getTerm());
    }

    @Test
    public void testRequestVote() {
        RequestVote expected = new RequestVote(1, "candidate", 2, 3);
        RequestVote actual = (RequestVote) SerializationUtils.clone(expected);
        assertEquals("getCandidateId", expected.getCandidateId(), actual.getCandidateId());
        assertEquals("getLastLogIndex", expected.getLastLogIndex(), actual.getLastLogIndex());
        assertEquals("getLastLogTerm", expected.getLastLogTerm(), actual.getLastLogTerm());
        assertEquals("getTerm", expected.getTerm(), actual.getTerm());
    }

    @Test
    public void testRequestVoteReply() {
        RequestVoteReply expected = new RequestVoteReply(1, true);
        RequestVoteReply actual = (RequestVoteReply) SerializationUtils.clone(expected);
        assertEquals("isVoteGranted", expected.isVoteGranted(), actual.isVoteGranted());
        assertEquals("getTerm", expected.getTerm(), actual.getTerm());
    }

    @Test
    public void testDeleteEntries() {
        DeleteEntries expected = new DeleteEntries(5);
        DeleteEntries actual = (DeleteEntries) SerializationUtils.clone(expected);
        assertEquals("getFromIndex", expected.getFromIndex(), actual.getFromIndex());
    }

    @Test
    public void testApplyLogEntries() {
        ApplyLogEntries expected = new ApplyLogEntries(5);
        ApplyLogEntries actual = (ApplyLogEntries) SerializationUtils.clone(expected);
        assertEquals("getToIndex", expected.getToIndex(), actual.getToIndex());
    }

    @Test
    public void testElectionTimeout() {
        SerializationUtils.clone(new ElectionTimeout());
    }
}
