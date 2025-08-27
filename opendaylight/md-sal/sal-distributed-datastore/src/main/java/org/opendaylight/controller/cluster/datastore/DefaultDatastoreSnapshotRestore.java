/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshotList;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class looks for a previously saved data store backup file in a directory and, if found, de-serializes
 * the DatastoreSnapshot instances. This class has a static singleton that is created on bundle activation.
 *
 * @author Thomas Pantelis
 */
@Beta
@Component(immediate = true)
public final class DefaultDatastoreSnapshotRestore implements DatastoreSnapshotRestore {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultDatastoreSnapshotRestore.class);

    private final Map<String, DatastoreSnapshot> datastoreSnapshots = new ConcurrentHashMap<>();
    private final String restoreDirectoryPath;

    public DefaultDatastoreSnapshotRestore() {
        this("./clustered-datastore-restore");
    }

    public DefaultDatastoreSnapshotRestore(final String restoreDirectoryPath) {
        this.restoreDirectoryPath = requireNonNull(restoreDirectoryPath);
    }

    @Override
    public Optional<DatastoreSnapshot> getAndRemove(final String datastoreType) {
        return Optional.ofNullable(datastoreSnapshots.remove(datastoreType));
    }

    @Activate
    @SuppressWarnings("checkstyle:IllegalCatch")
    void activate() {
        final var restoreDirectory = Path.of(restoreDirectoryPath);

        final Path[] files;
        try (var stream = Files.list(restoreDirectory)) {
            files = stream.toArray(Path[]::new);
        } catch (NoSuchFileException e) {
            if (!LOG.isTraceEnabled()) {
                e = null;
            }
            LOG.debug("Restore directory {} does not exist", restoreDirectory, e);
            return;
        } catch (IOException e) {
            LOG.warn("Could not list restore directory {}", restoreDirectory, e);
            return;
        }

        if (files.length == 0) {
            LOG.debug("Restore directory {} is empty", restoreDirectory);
            return;
        }
        if (files.length > 1) {
            LOG.error(
                "Found {} files in clustered datastore restore directory {} - expected 1. No restore will be attempted",
                files.length, restoreDirectory);
            return;
        }

        final var restoreFile = files[0];
        LOG.info("Clustered datastore will be restored from file {}", restoreFile);

        final DatastoreSnapshotList snapshots;
        try (var ois = new ObjectInputStream(Files.newInputStream(restoreFile))) {
            snapshots = (DatastoreSnapshotList) ois.readObject();
            LOG.debug("Deserialized {} snapshots", snapshots.size());
        } catch (ClassNotFoundException | IOException e) {
            LOG.error("Error reading clustered datastore restore file {}", restoreFile, e);
            return;
        } finally {
            try {
                Files.delete(restoreFile);
            } catch (IOException e) {
                LOG.error("Could not delete clustered datastore restore file {}", restoreFile, e);
            }
        }

        for (var snapshot : snapshots) {
            datastoreSnapshots.put(snapshot.getType(), snapshot);
        }
    }
}
