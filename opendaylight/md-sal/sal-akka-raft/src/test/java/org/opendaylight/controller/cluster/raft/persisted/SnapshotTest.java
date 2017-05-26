/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext.MockPayload;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;

/**
 * Unit tests for Snapshot.
 *
 * @author Thomas Pantelis
 */
public class SnapshotTest {

    @Test
    public void testSerialization() throws Exception {
        testSerialization(new byte[]{1, 2, 3, 4, 5, 6, 7}, Arrays.asList(
                new SimpleReplicatedLogEntry(6, 2, new MockPayload("payload"))));
        testSerialization(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9}, Collections.emptyList());
    }

    private void testSerialization(byte[] state, List<ReplicatedLogEntry> unapplied) throws Exception {
        long lastIndex = 6;
        long lastTerm = 2;
        long lastAppliedIndex = 5;
        long lastAppliedTerm = 1;
        long electionTerm = 3;
        String electionVotedFor = "member-1";
        ServerConfigurationPayload serverConfig = new ServerConfigurationPayload(Arrays.asList(
                new ServerInfo("1", true), new ServerInfo("2", false)));

        Snapshot expected = Snapshot.create(ByteState.of(state), unapplied, lastIndex, lastTerm, lastAppliedIndex,
                lastAppliedTerm, electionTerm, electionVotedFor, serverConfig);
        Snapshot cloned = (Snapshot) SerializationUtils.clone(expected);

        assertEquals("lastIndex", expected.getLastIndex(), cloned.getLastIndex());
        assertEquals("lastTerm", expected.getLastTerm(), cloned.getLastTerm());
        assertEquals("lastAppliedIndex", expected.getLastAppliedIndex(), cloned.getLastAppliedIndex());
        assertEquals("lastAppliedTerm", expected.getLastAppliedTerm(), cloned.getLastAppliedTerm());
        assertEquals("unAppliedEntries", expected.getUnAppliedEntries(), cloned.getUnAppliedEntries());
        assertEquals("electionTerm", expected.getElectionTerm(), cloned.getElectionTerm());
        assertEquals("electionVotedFor", expected.getElectionVotedFor(), cloned.getElectionVotedFor());
        assertEquals("state", expected.getState(), cloned.getState());
        assertEquals("serverConfig", expected.getServerConfiguration().getServerConfig(),
                cloned.getServerConfiguration().getServerConfig());
    }
}
