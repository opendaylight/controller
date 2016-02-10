/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.After;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.messages.DatastoreSnapshot;
import org.opendaylight.controller.cluster.datastore.messages.DatastoreSnapshot.ShardSnapshot;
import org.opendaylight.controller.cluster.datastore.messages.DatastoreSnapshotList;

/**
 * Unit tests for DatastoreSnapshotRestore.
 *
 * @author Thomas Pantelis
 */
public class DatastoreSnapshotRestoreTest {
    String restoreDirectoryPath = "target/DatastoreSnapshotRestoreTest-" + System.nanoTime();
    File restoreDirectoryFile = new File(restoreDirectoryPath);
    File backupFile = new File(restoreDirectoryFile, "backup");

    @After
    public void tearDown() {
        backupFile.delete();
        restoreDirectoryFile.delete();
    }

    @Test
    public void test() throws Exception {
        assertTrue("Failed to mkdir " + restoreDirectoryPath, restoreDirectoryFile.mkdirs());

        List<ShardSnapshot> shardSnapshots = new ArrayList<>();
        shardSnapshots.add(new ShardSnapshot("cars", new byte[]{1,2}));
        shardSnapshots.add(new ShardSnapshot("people", new byte[]{3,4}));
        DatastoreSnapshot configSnapshot = new DatastoreSnapshot("config", null, shardSnapshots );

        shardSnapshots = new ArrayList<>();
        shardSnapshots.add(new ShardSnapshot("cars", new byte[]{5,6}));
        shardSnapshots.add(new ShardSnapshot("people", new byte[]{7,8}));
        shardSnapshots.add(new ShardSnapshot("bikes", new byte[]{9,0}));
        DatastoreSnapshot operSnapshot = new DatastoreSnapshot("oper", null, shardSnapshots );

        DatastoreSnapshotList snapshotList = new DatastoreSnapshotList();
        snapshotList.add(configSnapshot);
        snapshotList.add(operSnapshot);

        File backupFile = new File(restoreDirectoryFile, "backup");
        try(FileOutputStream fos = new FileOutputStream(backupFile)) {
            SerializationUtils.serialize(snapshotList, fos);
        }

        DatastoreSnapshotRestore.createInstance(restoreDirectoryPath);

        verifySnapshot(configSnapshot, DatastoreSnapshotRestore.instance().getAndRemove("config"));
        verifySnapshot(operSnapshot, DatastoreSnapshotRestore.instance().getAndRemove("oper"));

        assertNull("DatastoreSnapshot was not removed", DatastoreSnapshotRestore.instance().getAndRemove("config"));

        assertFalse(backupFile + " was not deleted", backupFile.exists());

        DatastoreSnapshotRestore.removeInstance();
        DatastoreSnapshotRestore.createInstance("target/does-not-exist");
        assertNull("Expected null DatastoreSnapshot", DatastoreSnapshotRestore.instance().getAndRemove("config"));
        assertNull("Expected null DatastoreSnapshot", DatastoreSnapshotRestore.instance().getAndRemove("oper"));
    }

    private static void verifySnapshot(DatastoreSnapshot expected, DatastoreSnapshot actual) {
        assertNotNull("DatastoreSnapshot is null", actual);
        assertEquals("getType", expected.getType(), actual.getType());
        assertTrue("ShardManager snapshots don't match", Objects.deepEquals(expected.getShardManagerSnapshot(),
                actual.getShardManagerSnapshot()));
        assertEquals("ShardSnapshots size", expected.getShardSnapshots().size(), actual.getShardSnapshots().size());
        for(int i = 0; i < expected.getShardSnapshots().size(); i++) {
            assertEquals("ShardSnapshot " + (i + 1) + " name", expected.getShardSnapshots().get(i).getName(),
                    actual.getShardSnapshots().get(i).getName());
            assertArrayEquals("ShardSnapshot " + (i + 1) + " snapshot", expected.getShardSnapshots().get(i).getSnapshot(),
                    actual.getShardSnapshots().get(i).getSnapshot());
        }
    }
}
