/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.opendaylight.controller.cluster.datastore.messages.DatastoreSnapshot;
import org.opendaylight.controller.cluster.datastore.messages.DatastoreSnapshotList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class looks for a previously saved data store backup file in a directory and, if found, de-serializes
 * the DatastoreSnapshot instances. This class has a static singleton that is created on bundle activation.
 *
 * @author Thomas Pantelis
 */
public class DatastoreSnapshotRestore {
    private static final Logger LOG = LoggerFactory.getLogger(DatastoreSnapshotRestore.class);

    private static AtomicReference<DatastoreSnapshotRestore> instance = new AtomicReference<>();

    private final String restoreDirectoryPath;
    private final Map<String, DatastoreSnapshot> datastoreSnapshots = new ConcurrentHashMap<>();

    public static void createInstance(String restoreDirectoryPath) {
        instance.compareAndSet(null, new DatastoreSnapshotRestore(restoreDirectoryPath));
    }

    public static void removeInstance() {
        instance.set(null);
    }

    public static DatastoreSnapshotRestore instance() {
        DatastoreSnapshotRestore localInstance = instance.get();
        return Preconditions.checkNotNull(localInstance, "DatastoreSnapshotRestore instance was not created");
    }

    private DatastoreSnapshotRestore(String restoreDirectoryPath) {
        this.restoreDirectoryPath = Preconditions.checkNotNull(restoreDirectoryPath);
    }

    // sychronize this method so that, in case of concurrent access to getAndRemove(),
    // no one ends up with partially initialized data
    private synchronized void initialize() {

        File restoreDirectoryFile = new File(restoreDirectoryPath);

        String[] files = restoreDirectoryFile.list();
        if(files == null || files.length == 0) {
            LOG.debug("Restore directory {} does not exist or is empty", restoreDirectoryFile);
            return;
        }

        if(files.length > 1) {
            LOG.error("Found {} files in clustered datastore restore directory {} - expected 1. No restore will be attempted",
                    files.length, restoreDirectoryFile);
            return;
        }

        File restoreFile = new File(restoreDirectoryFile, files[0]);

        LOG.info("Clustered datastore will be restored from file {}", restoreFile);

        try(FileInputStream fis = new FileInputStream(restoreFile)) {
            DatastoreSnapshotList snapshots = deserialize(fis);
            LOG.debug("Deserialized {} snapshots", snapshots.size());

            for(DatastoreSnapshot snapshot: snapshots) {
                datastoreSnapshots.put(snapshot.getType(), snapshot);
            }
        } catch (Exception e) {
            LOG.error("Error reading clustered datastore restore file {}", restoreFile, e);
        } finally {
            if(!restoreFile.delete()) {
                LOG.error("Could not delete clustered datastore restore file {}", restoreFile);
            }
        }
    }

    private static DatastoreSnapshotList deserialize(InputStream inputStream) throws IOException, ClassNotFoundException {
        try(ObjectInputStream ois = new ObjectInputStream(inputStream)) {
            return (DatastoreSnapshotList) ois.readObject();
        }
    }

    public DatastoreSnapshot getAndRemove(String datastoreType) {
        initialize();
        return datastoreSnapshots.remove(datastoreType);
    }
}
