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
import java.util.List;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext.MockPayload;

/**
 * Unit tests for Snapshot.
 *
 * @author Thomas Pantelis
 */
public class SnapshotTest {

    @Test
    public void testBackwardsCompatibleDeserializationFromLithium() throws Exception {
        Snapshot expSnapshot = newLithiumSnapshot();
        try(FileInputStream fis = new FileInputStream("src/test/resources/lithium-serialized-Snapshot")) {
            ObjectInputStream ois = new ObjectInputStream(fis);

            Snapshot snapshot = (Snapshot) ois.readObject();
            ois.close();

            assertEquals("lastIndex", expSnapshot.getLastIndex(), snapshot.getLastIndex());
            assertEquals("lastTerm", expSnapshot.getLastTerm(), snapshot.getLastTerm());
            assertEquals("lastAppliedIndex", expSnapshot.getLastAppliedIndex(), snapshot.getLastAppliedIndex());
            assertEquals("lastAppliedTerm", expSnapshot.getLastAppliedTerm(), snapshot.getLastAppliedTerm());
            assertEquals("unAppliedEntries size", expSnapshot.getUnAppliedEntries().size(), snapshot.getUnAppliedEntries().size());
            assertArrayEquals("state", expSnapshot.getState(), snapshot.getState());
            assertEquals("electionTerm", 0, snapshot.getElectionTerm());
            assertEquals("electionVotedFor", null, snapshot.getElectionVotedFor());
        }
    }

    private static Snapshot newLithiumSnapshot() {
        byte[] state = {1, 2, 3, 4, 5};
        List<ReplicatedLogEntry> entries = new ArrayList<>();
        entries.add(new ReplicatedLogImplEntry(6, 2, new MockPayload("payload")));
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
    private static void generateSerializedFile(Snapshot snapshot, String fileName) throws IOException {
        FileOutputStream fos = new FileOutputStream("src/test/resources/" + fileName);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(snapshot);
        fos.close();
    }
}
