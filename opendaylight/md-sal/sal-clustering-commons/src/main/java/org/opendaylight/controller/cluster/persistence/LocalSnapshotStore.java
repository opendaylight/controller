/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.persistence;

import akka.actor.ExtendedActorSystem;
import akka.dispatch.Futures;
import akka.persistence.SelectedSnapshot;
import akka.persistence.SnapshotMetadata;
import akka.persistence.SnapshotSelectionCriteria;
import akka.persistence.serialization.Snapshot;
import akka.persistence.serialization.SnapshotSerializer;
import akka.persistence.snapshot.japi.SnapshotStore;
import akka.serialization.JavaSerializer;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.ByteStreams;
import com.typesafe.config.Config;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

/**
 * Akka SnapshotStore implementation backed by the local file system. This class was patterned after akka's
 * LocalSnapshotStore class and exists because akka's version serializes to a byte[] before persisting
 * to the file which will fail if the data reaches or exceeds Integer.MAX_VALUE in size. This class avoids that issue
 * by serializing the data directly to the file.
 *
 * @author Thomas Pantelis
 */
public class LocalSnapshotStore extends SnapshotStore {
    private static final Logger LOG = LoggerFactory.getLogger(LocalSnapshotStore.class);

    private static final int PERSISTENCE_ID_START_INDEX = "snapshot-".length();

    private final ExecutionContext executionContext;
    private final int maxLoadAttempts;
    private final File snapshotDir;

    public LocalSnapshotStore(final Config config) {
        this.executionContext = context().system().dispatchers().lookup(config.getString("stream-dispatcher"));
        snapshotDir = new File(config.getString("dir"));

        int localMaxLoadAttempts = config.getInt("max-load-attempts");
        maxLoadAttempts = localMaxLoadAttempts > 0 ? localMaxLoadAttempts : 1;

        LOG.debug("LocalSnapshotStore ctor: snapshotDir: {}, maxLoadAttempts: {}", snapshotDir, maxLoadAttempts);
    }

    @Override
    public void preStart() throws Exception {
        if (!snapshotDir.isDirectory()) {
            // Try to create the directory, on failure double check if someone else beat us to it.
            if (!snapshotDir.mkdirs() && !snapshotDir.isDirectory()) {
                throw new IOException("Failed to create snapshot directory " + snapshotDir.getCanonicalPath());
            }
        }

        super.preStart();
    }

    @Override
    public Future<Optional<SelectedSnapshot>> doLoadAsync(final String persistenceId,
                                                          final SnapshotSelectionCriteria criteria) {
        LOG.debug("In doLoadAsync - persistenceId: {}, criteria: {}", persistenceId, criteria);

        // Select the youngest 'maxLoadAttempts' snapshots that match the criteria. This may help in situations where
        // saving of a snapshot could not be completed because of a JVM crash. Hence, an attempt to load that snapshot
        // will fail but loading an older snapshot may succeed.

        Deque<SnapshotMetadata> metadatas = getSnapshotMetadatas(persistenceId, criteria).stream()
                .sorted(LocalSnapshotStore::compare).collect(reverse()).stream().limit(maxLoadAttempts)
                    .collect(Collectors.toCollection(ArrayDeque::new));

        if (metadatas.isEmpty()) {
            return Futures.successful(Optional.empty());
        }

        LOG.debug("doLoadAsync - found: {}", metadatas);

        return Futures.future(() -> doLoad(metadatas), executionContext);
    }

    private Optional<SelectedSnapshot> doLoad(final Deque<SnapshotMetadata> metadatas) throws IOException {
        SnapshotMetadata metadata = metadatas.removeFirst();
        File file = toSnapshotFile(metadata);

        LOG.debug("doLoad {}", file);

        try {
            Object data = deserialize(file);

            LOG.debug("deserialized data: {}", data);

            return Optional.of(new SelectedSnapshot(metadata, data));
        } catch (IOException e) {
            LOG.error("Error loading snapshot file {}, remaining attempts: {}", file, metadatas.size(), e);

            if (metadatas.isEmpty()) {
                throw e;
            }

            return doLoad(metadatas);
        }
    }

    private Object deserialize(final File file) throws IOException {
        return JavaSerializer.currentSystem().withValue((ExtendedActorSystem) context().system(),
            (Callable<Object>) () -> {
                try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
                    return in.readObject();
                } catch (ClassNotFoundException e) {
                    throw new IOException("Error loading snapshot file " + file, e);
                } catch (IOException e) {
                    LOG.debug("Error loading snapshot file {}", file, e);
                    return tryDeserializeAkkaSnapshot(file);
                }
            });
    }

    private Object tryDeserializeAkkaSnapshot(final File file) throws IOException {
        LOG.debug("tryDeserializeAkkaSnapshot {}", file);

        // The snapshot was probably previously stored via akka's LocalSnapshotStore which wraps the data
        // in a Snapshot instance and uses the SnapshotSerializer to serialize it to a byte[]. So we'll use
        // the SnapshotSerializer to try to de-serialize it.

        SnapshotSerializer snapshotSerializer = new SnapshotSerializer((ExtendedActorSystem) context().system());

        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            return ((Snapshot)snapshotSerializer.fromBinary(ByteStreams.toByteArray(in))).data();
        }
    }

    @Override
    public Future<Void> doSaveAsync(final SnapshotMetadata metadata, final Object snapshot) {
        LOG.debug("In doSaveAsync - metadata: {}, snapshot: {}", metadata, snapshot);

        return Futures.future(() -> doSave(metadata, snapshot), executionContext);
    }

    private Void doSave(final SnapshotMetadata metadata, final Object snapshot) throws IOException {
        final File actual = toSnapshotFile(metadata);
        final File temp = File.createTempFile(actual.getName(), null, snapshotDir);

        LOG.debug("Saving to temp file: {}", temp);

        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(temp))) {
            out.writeObject(snapshot);
        } catch (IOException e) {
            LOG.error("Error saving snapshot file {}. Deleting file..", temp, e);
            if (!temp.delete()) {
                LOG.error("Failed to successfully delete file {}", temp);
            }
            throw e;
        }

        LOG.debug("Renaming to: {}", actual);
        try {
            Files.move(temp.toPath(), actual.toPath(), StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            LOG.warn("Failed to move {} to {}. Deleting {}..", temp, actual, temp, e);
            if (!temp.delete()) {
                LOG.error("Failed to successfully delete file {}", temp);
            }
            throw e;
        }

        return null;
    }

    @Override
    public Future<Void> doDeleteAsync(final SnapshotMetadata metadata) {
        LOG.debug("In doDeleteAsync - metadata: {}", metadata);

        // Multiple snapshot files here mean that there were multiple snapshots for this seqNr - we delete all of them.
        // Usually snapshot-stores would keep one snapshot per sequenceNr however here in the file-based one we
        // timestamp snapshots and allow multiple to be kept around (for the same seqNr) if desired.

        return Futures.future(() -> doDelete(metadata), executionContext);
    }

    @Override
    public Future<Void> doDeleteAsync(final String persistenceId, final SnapshotSelectionCriteria criteria) {
        LOG.debug("In doDeleteAsync - persistenceId: {}, criteria: {}", persistenceId, criteria);

        return Futures.future(() -> doDelete(persistenceId, criteria), executionContext);
    }

    private Void doDelete(final String persistenceId, final SnapshotSelectionCriteria criteria) {
        final List<File> files = getSnapshotMetadatas(persistenceId, criteria).stream()
                .flatMap(md -> Stream.of(toSnapshotFile(md))).collect(Collectors.toList());

        LOG.debug("Deleting files: {}", files);

        files.forEach(File::delete);
        return null;
    }

    private Void doDelete(final SnapshotMetadata metadata) {
        final Collection<File> files = getSnapshotFiles(metadata);

        LOG.debug("Deleting files: {}", files);

        files.forEach(File::delete);
        return null;
    }

    private Collection<File> getSnapshotFiles(final String persistenceId) {
        String encodedPersistenceId = encode(persistenceId);

        File[] files = snapshotDir.listFiles((dir, name) -> {
            int persistenceIdEndIndex = name.lastIndexOf('-', name.lastIndexOf('-') - 1);
            return PERSISTENCE_ID_START_INDEX + encodedPersistenceId.length() == persistenceIdEndIndex
                    && name.startsWith(encodedPersistenceId, PERSISTENCE_ID_START_INDEX) && !name.endsWith(".tmp");
        });

        if (files == null) {
            return Collections.emptyList();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("getSnapshotFiles for persistenceId: {}, found files: {}", encodedPersistenceId,
                    Arrays.toString(files));
        }

        return Arrays.asList(files);
    }

    private Collection<File> getSnapshotFiles(final SnapshotMetadata metadata) {
        return getSnapshotFiles(metadata.persistenceId()).stream().filter(file -> {
            SnapshotMetadata possible = extractMetadata(file);
            return possible != null && possible.sequenceNr() == metadata.sequenceNr()
                    && (metadata.timestamp() == 0L || possible.timestamp() == metadata.timestamp());
        }).collect(Collectors.toList());
    }

    private Collection<SnapshotMetadata> getSnapshotMetadatas(final String persistenceId,
            final SnapshotSelectionCriteria criteria) {
        return getSnapshotFiles(persistenceId).stream().flatMap(file -> toStream(extractMetadata(file)))
                .filter(criteria::matches).collect(Collectors.toList());
    }

    private static Stream<SnapshotMetadata> toStream(@Nullable final SnapshotMetadata md) {
        return md != null ? Stream.of(md) : Stream.empty();
    }

    @Nullable
    private static SnapshotMetadata extractMetadata(final File file) {
        String name = file.getName();
        int sequenceNumberEndIndex = name.lastIndexOf('-');
        int persistenceIdEndIndex = name.lastIndexOf('-', sequenceNumberEndIndex - 1);
        if (PERSISTENCE_ID_START_INDEX >= persistenceIdEndIndex) {
            return null;
        }

        try {
            // Since the persistenceId is url encoded in the filename, we need
            // to decode relevant filename's part to obtain persistenceId back
            String persistenceId = decode(name.substring(PERSISTENCE_ID_START_INDEX, persistenceIdEndIndex));
            long sequenceNumber = Long.parseLong(name.substring(persistenceIdEndIndex + 1, sequenceNumberEndIndex));
            long timestamp = Long.parseLong(name.substring(sequenceNumberEndIndex + 1));
            return new SnapshotMetadata(persistenceId, sequenceNumber, timestamp);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private File toSnapshotFile(final SnapshotMetadata metadata) {
        return new File(snapshotDir, String.format("snapshot-%s-%d-%d", encode(metadata.persistenceId()),
            metadata.sequenceNr(), metadata.timestamp()));
    }

    private static <T> Collector<T, ?, List<T>> reverse() {
        return Collectors.collectingAndThen(Collectors.toList(), list -> {
            Collections.reverse(list);
            return list;
        });
    }

    private static String encode(final String str) {
        try {
            return URLEncoder.encode(str, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            // Shouldn't happen
            LOG.warn("Error encoding {}", str, e);
            return str;
        }
    }

    private static String decode(final String str) {
        try {
            return URLDecoder.decode(str, StandardCharsets.UTF_8.name());
        } catch (final UnsupportedEncodingException e) {
            // Shouldn't happen
            LOG.warn("Error decoding {}", str, e);
            return str;
        }
    }

    @VisibleForTesting
    static int compare(final SnapshotMetadata m1, final SnapshotMetadata m2) {
        return (int) (!m1.persistenceId().equals(m2.persistenceId())
                ? m1.persistenceId().compareTo(m2.persistenceId()) :
            m1.sequenceNr() != m2.sequenceNr() ? m1.sequenceNr() - m2.sequenceNr() :
                m1.timestamp() != m2.timestamp() ? m1.timestamp() - m2.timestamp() : 0);
    }
}
