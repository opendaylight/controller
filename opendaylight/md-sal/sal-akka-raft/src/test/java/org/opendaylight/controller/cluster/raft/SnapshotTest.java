/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext.MockPayload;
import org.opendaylight.controller.cluster.raft.persisted.ServerConfigurationPayload;
import org.opendaylight.controller.cluster.raft.persisted.ServerInfo;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;

/**
 * Unit tests for Snapshot.
 *
 * @author Thomas Pantelis
 */
@Deprecated
public class SnapshotTest {

    @Test
    public void testSerialization() throws Exception {
        long lastIndex = 6;
        long lastTerm = 2;
        long lastAppliedIndex = 5;
        long lastAppliedTerm = 1;
        long electionTerm = 3;
        String electionVotedFor = "member-1";
        ServerConfigurationPayload serverConfig = new ServerConfigurationPayload(Arrays.asList(
                new ServerInfo("1", true), new ServerInfo("2", false)));
        List<ReplicatedLogEntry> unapplied = Arrays.asList(
                new SimpleReplicatedLogEntry(6, 2, new MockPayload("payload")));
        byte[] state = {1, 2, 3, 4, 5, 6, 7};

        Snapshot expected = Snapshot.create(state, unapplied, lastIndex, lastTerm, lastAppliedIndex,
                lastAppliedTerm, electionTerm, electionVotedFor, serverConfig);
        org.opendaylight.controller.cluster.raft.persisted.Snapshot cloned =
                (org.opendaylight.controller.cluster.raft.persisted.Snapshot) SerializationUtils.clone(expected);

        assertEquals("lastIndex", expected.getLastIndex(), cloned.getLastIndex());
        assertEquals("lastTerm", expected.getLastTerm(), cloned.getLastTerm());
        assertEquals("lastAppliedIndex", expected.getLastAppliedIndex(), cloned.getLastAppliedIndex());
        assertEquals("lastAppliedTerm", expected.getLastAppliedTerm(), cloned.getLastAppliedTerm());
        assertEquals("unAppliedEntries", expected.getUnAppliedEntries(), cloned.getUnAppliedEntries());
        assertEquals("electionTerm", expected.getElectionTerm(), cloned.getElectionTerm());
        assertEquals("electionVotedFor", expected.getElectionVotedFor(), cloned.getElectionVotedFor());
        assertArrayEquals("state", expected.getState(), cloned.getState().read());
        assertEquals("serverConfig", expected.getServerConfiguration().getServerConfig(),
                cloned.getServerConfiguration().getServerConfig());
        assertEquals("isMigrated", true, cloned.isMigrated());
    }

    @Test
    public void testBackwardsCompatibleDeserializationFromLithium() throws Exception {
        Snapshot expSnapshot = newLithiumSnapshot();
        try (FileInputStream fis = new FileInputStream("src/test/resources/lithium-serialized-Snapshot")) {
            ObjectInputStream ois = new ObjectInputStream(fis);

            org.opendaylight.controller.cluster.raft.persisted.Snapshot snapshot =
                    (org.opendaylight.controller.cluster.raft.persisted.Snapshot) ois.readObject();
            ois.close();

            assertEquals("lastIndex", expSnapshot.getLastIndex(), snapshot.getLastIndex());
            assertEquals("lastTerm", expSnapshot.getLastTerm(), snapshot.getLastTerm());
            assertEquals("lastAppliedIndex", expSnapshot.getLastAppliedIndex(), snapshot.getLastAppliedIndex());
            assertEquals("lastAppliedTerm", expSnapshot.getLastAppliedTerm(), snapshot.getLastAppliedTerm());
            assertEquals("unAppliedEntries size", expSnapshot.getUnAppliedEntries().size(),
                    snapshot.getUnAppliedEntries().size());
            assertArrayEquals("state", expSnapshot.getState(), snapshot.getState().read());
            assertEquals("electionTerm", 0, snapshot.getElectionTerm());
            assertEquals("electionVotedFor", null, snapshot.getElectionVotedFor());
        }
    }

    private static Snapshot newLithiumSnapshot() {
        byte[] state = {1, 2, 3, 4, 5};
        List<ReplicatedLogEntry> entries = new ArrayList<>();
        entries.add(new org.opendaylight.controller.cluster.raft.ReplicatedLogImplEntry(
                6, 2, new MockPayload("payload")));
        long lastIndex = 6;
        long lastTerm = 2;
        long lastAppliedIndex = 5;
        long lastAppliedTerm = 1;

        return Snapshot.create(state, entries, lastIndex, lastTerm, lastAppliedIndex, lastAppliedTerm);
    }

    /**
     * Use this method to generate a file with a serialized Snapshot instance to be
     * used in tests that verify backwards compatible de-serialization.
     */
    @SuppressWarnings("unused")
    private static void generateSerializedFile(Snapshot snapshot, String fileName) throws IOException {
        FileOutputStream fos = new FileOutputStream("src/test/resources/" + fileName);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(snapshot);
        fos.close();
    }
}
