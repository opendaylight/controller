/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import com.google.common.base.CharMatcher;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.primitives.UnsignedLong;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.apache.pekko.persistence.JournalProtocol;
import org.apache.pekko.persistence.SnapshotProtocol;
import org.apache.pekko.persistence.SnapshotSelectionCriteria;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.raft.spi.SnapshotFileFormat;
import org.opendaylight.raft.spi.SnapshotSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link EnabledRaftStorage} backed by a local directory.
 */
@NonNullByDefault
public final class LocalRaftStorage extends EnabledRaftStorage {
    @Beta
    public record Configuration(SnapshotFileFormat preferredFormat) {
        public Configuration {
            requireNonNull(preferredFormat);
        }

        public Configuration() {
            this(SnapshotFileFormat.PLAIN);
        }
    }

    // A file we have recognized to be a valid snapshot
    private record SnapshotFile(Path path, SnapshotFileFormat format, UnsignedLong sequence) {
        SnapshotFile {
            requireNonNull(path);
            requireNonNull(format);
            requireNonNull(sequence);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(LocalRaftStorage.class);
    private static final String FILENAME_START_STR = "snapshot-";
    private static final int FILENAME_START_LEN = FILENAME_START_STR.length();
    private static final CharMatcher DIGIT_MATCHER = CharMatcher.inRange('0', '9');

    private final Configuration config;
    private final String memberId;
    private final Path directory;

    private UnsignedLong nextSequence = UnsignedLong.ZERO;

    public LocalRaftStorage(final String memberId, final Path directory, final Configuration config) {
        this.memberId = requireNonNull(memberId);
        this.directory = requireNonNull(directory);
        this.config = requireNonNull(config);
    }


    @Override
    protected void postStart() throws IOException {
        // FIXME: at least
        //   - creates the directory if not present
        //   - creates a 'lock' file and java.nio.channels.FileChannel.tryLock()s it
        //   - scans the directory to:
        //     - clean up any temporary files
        //     - determine nextSequence
    }

    @Override
    protected void preStop() {
        // TODO Auto-generated method stub
        //   - terminates any incomplete operations, reporting a CancellationException to them
        //   - unlocks the file
    }


    // FIXME: more things:
    // - stop() that:
    // - a lava.lang.ref.Clearner which does the same as stop()
    // For scalability this locking should really be done on top-level stateDir so we have one file open for all shards,
    // not one file per shard.

    @Override
    public @Nullable SnapshotSource tryLatestSnapshot() throws IOException {
        final var files = listFiles();
        if (files.isEmpty()) {
            LOG.debug("{}: no eligible files found", memberId);
            return null;
        }

        final var lastSequence = files.getLast().sequence;
        if (lastSequence.compareTo(nextSequence) >= 0) {
            final var updated = lastSequence.plus(UnsignedLong.ONE);
            LOG.debug("{}: updated next sequence from {} to {}", memberId, nextSequence, updated);
            nextSequence = updated;
        }

        final var first = files.getFirst();
        LOG.debug("{}: picked {} as the latest file", memberId, first);
        return first.format.sourceForFile(first.path());
    }

    private List<SnapshotFile> listFiles() throws IOException {
        if (!Files.exists(directory)) {
            LOG.debug("{}: directory {} does not exist", memberId, directory);
            return List.of();
        }

        final List<SnapshotFile> ret;
        try (var paths = Files.list(directory)) {
            ret = paths.map(this::pathToFile).filter(Objects::nonNull)
                .sorted(Comparator.comparing(SnapshotFile::sequence))
                .toList();
        }
        LOG.trace("{}: recognized files: {}", memberId, ret);
        return ret;
    }

    private @Nullable SnapshotFile pathToFile(final Path path) {
        if (!Files.isRegularFile(path)) {
            LOG.debug("{}: skipping non-file {}", memberId, path);
            return null;
        }
        if (!Files.isReadable(path)) {
            LOG.debug("{}: skipping unreadable file {}", memberId, path);
            return null;
        }
        final var name = path.getFileName().toString();
        if (!name.startsWith(FILENAME_START_STR)) {
            LOG.debug("{}: skipping known file {}", memberId, path);
            return null;
        }
        final var format = SnapshotFileFormat.forFileName(name);
        if (format == null) {
            LOG.debug("{}: skipping unhandled file {}", memberId, path);
            return null;
        }
        LOG.debug("{}: selected {} to handle file {}", memberId, format, path);

        // Strip the extension and parse sequence number
        final var seqStr = name.substring(FILENAME_START_LEN, name.length() - format.extension().length());
        LOG.trace("{}: parsing {} as a sequence number", memberId, seqStr);
        if (!DIGIT_MATCHER.matchesAllOf(FILENAME_START_STR)) {
            LOG.warn("{}: ignoring badly-named file {}", memberId, path);
            return null;
        }

        final long seqBits;
        try {
            seqBits = Long.parseUnsignedLong(seqStr);
        } catch (NumberFormatException e) {
            LOG.warn("{}: ignoring unparseable index in file {}", memberId, path, e);
            return null;
        }

        return new SnapshotFile(path, format, UnsignedLong.fromLongBits(seqBits));
    }

    @Override
    public <T> void persist(final T entry, final Consumer<T> callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> void persistAsync(final T entry, final Consumer<T> callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void saveSnapshot(final Snapshot snapshot) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteSnapshots(final SnapshotSelectionCriteria criteria) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteMessages(final long sequenceNumber) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLastSequenceNumber() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean handleJournalResponse(final JournalProtocol.Response response) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean handleSnapshotResponse(final SnapshotProtocol.Response response) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected String memberId() {
        return memberId;
    }

    @Override
    protected ToStringHelper addToStringAtrributes(final ToStringHelper helper) {
        return super.addToStringAtrributes(helper).add("directory", directory).add("format", config.preferredFormat);
    }
}
