/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.persistence;

import akka.persistence.SnapshotMetadata;
import akka.persistence.SnapshotOffer;
import akka.persistence.SnapshotSelectionCriteria;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.io.InputOutputStreamFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewLocalSnapshotStore {
    private static final Logger LOG = LoggerFactory.getLogger(NewLocalSnapshotStore.class);
    private static final int PERSISTENCE_ID_START_INDEX = "snapshot-".length();

    private final InputOutputStreamFactory streamFactory;
    private final int maxLoadAttempts;
    private final File snapshotDir;

    @SuppressFBWarnings(value = "MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR", justification = "Akka class design")
    public NewLocalSnapshotStore(final String lz4BlockSize, final String dir, int newMaxLoadAttempts,
                              final boolean useLz4Compression) {
        snapshotDir = new File(dir);

        maxLoadAttempts = newMaxLoadAttempts > 0 ? newMaxLoadAttempts : 1;

        if (useLz4Compression) {
            streamFactory = InputOutputStreamFactory.lz4(lz4BlockSize);
            LOG.debug("Using LZ4 Input/Output Stream, blocksize: {}", lz4BlockSize);
        } else {
            streamFactory = InputOutputStreamFactory.simple();
            LOG.debug("Using plain Input/Output Stream");
        }
        preStart();
        LOG.debug("LocalSnapshotStore ctor: snapshotDir: {}, maxLoadAttempts: {}", snapshotDir, maxLoadAttempts);
    }


    public void preStart() {
        if (!snapshotDir.isDirectory()) {
            // Try to create the directory, on failure double check if someone else beat us to it.
            if (!snapshotDir.mkdirs()) {
                throw new IllegalStateException("Failed to create snapshot directory " + snapshotDir);
            }
        }
    }

    public ListenableFuture<Optional<SnapshotOffer>> doLoadAsync(final String persistenceId,
                                                                    final SnapshotSelectionCriteria criteria) {
        LOG.debug("In doLoadAsync - persistenceId: {}, criteria: {}", persistenceId, criteria);

        // Select the youngest 'maxLoadAttempts' snapshots that match the criteria. This may help in situations where
        // saving of a snapshot could not be completed because of a JVM crash. Hence, an attempt to load that snapshot
        // will fail but loading an older snapshot may succeed.

        Deque<SnapshotMetadata> metadatas = getSnapshotMetadatas(persistenceId, criteria).stream()
                .sorted(LocalSnapshotStore::compare).collect(reverse()).stream().limit(maxLoadAttempts)
                .collect(Collectors.toCollection(ArrayDeque::new));

        if (metadatas.isEmpty()) {
            return Futures.immediateFuture(Optional.empty());
        }

        LOG.debug("doLoadAsync - found: {}", metadatas);
        return Futures.submit(() -> doLoad(metadatas), Executors.newSingleThreadExecutor());
    }

    private Optional<SnapshotOffer> doLoad(final Deque<SnapshotMetadata> metadatas) throws IOException {
        SnapshotMetadata metadata = metadatas.removeFirst();
        File file = toSnapshotFile(metadata);

        LOG.debug("doLoad {}", file);

        try {
            Object data = deserialize(file);

            LOG.debug("deserialized data: {}", data);

            return Optional.of(new SnapshotOffer(metadata, data));
        } catch (IOException e) {
            LOG.error("Error loading snapshot file {}, remaining attempts: {}", file, metadatas.size(), e);

            if (metadatas.isEmpty()) {
                throw e;
            }

            return doLoad(metadatas);
        }
    }

    private Object deserialize(final File file) throws IOException {
        try (ObjectInputStream in = new ObjectInputStream(streamFactory.createInputStream(file))) {
            return in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Error loading snapshot file " + file, e);
        } catch (IOException e) {
            LOG.debug("Error loading snapshot file {}", file, e);
            //TODO: some solution fo legacy serialization
            throw new IOException("Error loading snapshot file " + file, e);
        }
    }

    public ListenableFuture<SnapshotMetadata> doSaveAsync(final SnapshotMetadata metadata, final Object snapshot) {
        LOG.debug("In doSaveAsync - metadata: {}, snapshot: {}", metadata, snapshot);

        return Futures.submit(() -> doSave(metadata, snapshot), Executors.newSingleThreadExecutor());
    }

    private SnapshotMetadata doSave(final SnapshotMetadata metadata, final Object snapshot) throws IOException {
        final File actual = toSnapshotFile(metadata);
        final File temp = File.createTempFile(actual.getName(), null, snapshotDir);

        LOG.debug("Saving to temp file: {}", temp);

        try (ObjectOutputStream out = new ObjectOutputStream(streamFactory.createOutputStream(temp))) {
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

        return metadata;
    }

//    public Future<Void> doDeleteAsync(final SnapshotMetadata metadata) {
//        LOG.debug("In doDeleteAsync - metadata: {}", metadata);
//
//        //Multiple snapshot files here mean that there were multiple snapshots for this seqNr - we delete all of them.
//        // Usually snapshot-stores would keep one snapshot per sequenceNr however here in the file-based one we
//        // timestamp snapshots and allow multiple to be kept around (for the same seqNr) if desired.
//
//        return Futures.submit(() -> doDelete(metadata), Executors.newSingleThreadExecutor());
//    }


    public Future<Void> doDeleteAsync(final String persistenceId, final SnapshotSelectionCriteria criteria) {
        LOG.debug("In doDeleteAsync - persistenceId: {}, criteria: {}", persistenceId, criteria);

        return Futures.submit(() -> doDelete(persistenceId, criteria), Executors.newSingleThreadExecutor());
    }

    private Void doDelete(final String persistenceId, final SnapshotSelectionCriteria criteria) {
        final List<File> files = getSnapshotMetadatas(persistenceId, criteria).stream()
                .flatMap(md -> Stream.of(toSnapshotFile(md))).collect(Collectors.toList());

        LOG.debug("Deleting files: {}", files);

        files.forEach(file -> {
            try {
                Files.delete(file.toPath());
            } catch (IOException | SecurityException e) {
                LOG.error("Unable to delete snapshot file: {}, persistenceId: {} ", file, persistenceId);
            }
        });
        return null;
    }
//
//    private Void doDelete(final SnapshotMetadata metadata) {
//        final Collection<File> files = getSnapshotFiles(metadata);
//
//        LOG.debug("Deleting files: {}", files);
//
//        files.forEach(file -> {
//            try {
//                Files.delete(file.toPath());
//            } catch (IOException | SecurityException e) {
//                LOG.error("Unable to delete snapshot file: {}", file);
//            }
//        });
//        return null;
//    }

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

//    private Collection<File> getSnapshotFiles(final SnapshotMetadata metadata) {
//        return getSnapshotFiles(metadata.persistenceId()).stream().filter(file -> {
//            SnapshotMetadata possible = extractMetadata(file);
//            return possible != null && possible.sequenceNr() == metadata.sequenceNr()
//                    && (metadata.timestamp() == 0L || possible.timestamp() == metadata.timestamp());
//        }).collect(Collectors.toList());
//    }

    private Collection<SnapshotMetadata> getSnapshotMetadatas(final String persistenceId,
                                                              final SnapshotSelectionCriteria criteria) {
        return getSnapshotFiles(persistenceId).stream().flatMap(file -> toStream(extractMetadata(file)))
                .filter(criteria::matches).collect(Collectors.toList());
    }

    private static Stream<SnapshotMetadata> toStream(final @Nullable SnapshotMetadata md) {
        return md != null ? Stream.of(md) : Stream.empty();
    }

    private static @Nullable SnapshotMetadata extractMetadata(final File file) {
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
        return URLEncoder.encode(str, StandardCharsets.UTF_8);
    }

    private static String decode(final String str) {
        return URLDecoder.decode(str, StandardCharsets.UTF_8);
    }

//    @VisibleForTesting
//    static int compare(final SnapshotMetadata m1, final SnapshotMetadata m2) {
//        checkArgument(m1.persistenceId().equals(m2.persistenceId()),
//                "Persistence id does not match. id1: %s, id2: %s", m1.persistenceId(), m2.persistenceId());
//        final int cmp = Long.compare(m1.timestamp(), m2.timestamp());
//        return cmp != 0 ? cmp : Long.compare(m1.sequenceNr(), m2.sequenceNr());
//    }
}
