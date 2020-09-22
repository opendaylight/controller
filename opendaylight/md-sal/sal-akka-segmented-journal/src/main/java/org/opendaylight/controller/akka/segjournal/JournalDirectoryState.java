/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.akka.segjournal;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.SYNC;
import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In-memory representation of the state file in a particular directory. This is nominally held in
 * {@code $DIR/state.bin} and is always manipulated using atomic operations through {@code $DIR/state.tmp}.
 *
 * <p>
 * There are a number of states a directory can be in. There are three steady states, in which the data files are
 * completely consistent:
 * <ul>
 *   <li>{@link NonExistent}, i.e. it will end up being created when the first persistence operation occurs,</li>
 *   <li>{@link Legacy}, i.e. the directory exists, but it does not have a state file or the state file indicates a
 *       legacy directory organization.</li>
 *   <li>{@link Current}, i.e. the the directory exists and its organization corresponds to the latest known format</li>
 * </ul>
 * If a legacy organization is detected, it will need to be upgraded before it can be used. The upgrade process can tax
 * the IO subsystem and may take a significant amount of time -- depending on the details of the upgrade.
 *
 * <p>
 * We currently support two directory organization layouts:
 * <ul>
 *   <li>{@code version 0}, which does not have a state file and stores every journal entry as a single segmented file
 *       entry in the {@code data} segmented file. This layout offers relatively simple access and minimum metadata
 *       overhead, but imposes a (configurable) maximum limit on how big a single journal entry is. Furthermore it also
 *       imposes a memory overhead of 2x the maximum entry size (i.e. 32MiB heap for 16MiB maximum entry size).</li>
 *   <li>{@code Version 1}, which has a version 1 state file and stores journal entries in one or more segmented file
 *       entries in the {@code fragdata} segmented file. This segmented file uses small maximum entry size (128KiB) and
 *       if a journal entry is larger, it is stored as multiple consecutive fragments. This layout imposes some
 *       additional complexity and bookkeeping overhead, but removes the limit on maximum journal entry size and places
 *       ensures the memory overhead in kept reasonable (256KiB).</li>
 * </ul>
 * We thus have only one upgrade path, which is represented through following transient directory states:
 * <ul>
 *   <li>{@link FragmentingEntries}, during which journal entries are read from {@code data} segmented file and written
 *       into the {@code fragdata} file. This process is checkpointed after each {@code data} segment is processed. Each
 *       checkpoint requires fully synchronizing the {@code fragdata} file and writing out a new state file. Should a
 *       restart or similar happen while fragmentation is happening, the process will restart from the last successfully
 *       converted segment. Once the last segment is converted and the corresponding checkpoint is made, the directory
 *       will transition to the next state,</li>
 *   <li>{@link DeletingDataSegFile}, during which the {@code fragdata} holds the primary copy of the data and
 *       {@code data} segmented file is being removed. Once the files are removed and the filesystem is properly
 *       synchronized, the directory will transition to {@link Current} state and normal operation resumes.</li>
 * </ul>
 *
 * <p>
 * Note that while this upgrade process is potentially reversible, we do not provide the tools to perform the downgrade.
 */
abstract class JournalDirectoryState {
    static final class NonExistent extends JournalDirectoryState {
        private NonExistent(final File directory) {
            super(directory);
        }

        @Override
        void serializeTo(final DataOutput out) throws IOException {
            // Same as Current, i.e. all new directories are created with new layour
            out.writeByte(ST_DIRLAYOUT_ONE);
        }
    }

    static final class Legacy extends JournalDirectoryState {
        private Legacy(final File directory) {
            super(directory);
        }

        @Override
        void serializeTo(final DataOutput out) throws IOException {
            out.writeByte(ST_DIRLAYOUT_ZERO);
        }

        FragmentingEntries startUpgrade() throws IOException {
            final FragmentingEntries ret = new FragmentingEntries(directory(), 0, 0);
            ret.checkpoint();
            return ret;
        }
    }

    static final class Current extends JournalDirectoryState {
        private Current(final File directory) {
            super(directory);
        }

        @Override
        void serializeTo(final DataOutput out) throws IOException {
            out.writeByte(ST_DIRLAYOUT_ONE);
        }
    }

    static final class FragmentingEntries extends JournalDirectoryState {
        private final long nextDataEntry;
        private final long nextFragDataEntry;

        private FragmentingEntries(final File directory, final long nextDataEntry, final long nextFragDataEntry) {
            super(directory);
            this.nextDataEntry = nextDataEntry;
            this.nextFragDataEntry = nextFragDataEntry;
        }

        /**
         * Return the next entry in the {@code data} segmented file which needs to be processed. If no entries have been
         * processed, this will be {@code 0}. If all entries have been processed, this will be one greater than the
         * number of entries in the segmented file.
         *
         * @return The next unfragmented segmented file entry that should be processed.
         */
        long nextDataEntry() {
            return nextDataEntry;
        }

        /**
         * Return the next entry in the {@code fragdata} segmented file which should be written. If no entries have been
         * processed, this will be {@code 0}. If all entries have been processed, this will be one greater than the
         * total number of entries in the segmented file (i.e. journal entry fragments).
         *
         * <p>
         * When upgrade process starts, or restarts, this entry in {@code fragdata} segmented file and all subsequent
         * entries are discarded before any journal entry is processed.
         *
         * @return The next fragmented segmented file entry that should be written.
         */
        long nextFragDataEntry() {
            return nextFragDataEntry;
        }

        FragmentingEntries finishSegmentAt(final long newNextDataEntry, final long newNextFragDataEntry)
                throws IOException {
            final FragmentingEntries ret = new FragmentingEntries(directory(), newNextDataEntry, newNextFragDataEntry);
            ret.checkpoint();
            return ret;
        }

        DeletingDataSegFile startCleanup() throws IOException {
            final DeletingDataSegFile ret = new DeletingDataSegFile(directory());
            ret.checkpoint();
            return ret;
        }

        @Override
        void serializeTo(final DataOutput out) throws IOException {
            out.writeByte(ST_DIRLAYOUT_ZERO_ONE_PHASE1);
            out.writeLong(nextDataEntry);
            out.writeLong(nextFragDataEntry);
        }

        @Override
        ToStringHelper addToStringAttributes(final ToStringHelper helper) {
            return helper.add("nextDataEntry", nextDataEntry).add("nextFragDataEntry", nextFragDataEntry);
        }
    }

    static final class DeletingDataSegFile extends JournalDirectoryState {
        private DeletingDataSegFile(final File directory) {
            super(directory);
        }

        Current finishUpgrade() throws IOException {
            final Current ret = new Current(directory());
            ret.checkpoint();
            return ret;
        }

        @Override
        void serializeTo(final DataOutput out) throws IOException {
            out.writeByte(ST_DIRLAYOUT_ZERO_ONE_PHASE2);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(JournalDirectoryState.class);
    private static final int FILE_MAGIC = 0x125dd274;
    private static final byte STATE_VERSION_ONE = 1;

    // State version one constants. We host them here as long we do not have an alternative.
    // The first byte is one of these:
    // - steady-state old layout
    private static final byte ST_DIRLAYOUT_ZERO            = 1;
    // - steady-state new layout
    private static final byte ST_DIRLAYOUT_ONE             = 2;
    // - old-to-new migration: migrate entries
    private static final byte ST_DIRLAYOUT_ZERO_ONE_PHASE1 = 3;
    // - old-to-new migration: delete old files
    private static final byte ST_DIRLAYOUT_ZERO_ONE_PHASE2 = 4;

    private final File directory;

    JournalDirectoryState(final File directory) {
        this.directory = requireNonNull(directory);
    }

    static JournalDirectoryState forDirectory(final File directory) throws IOException {
        if (!directory.exists()) {
            return new NonExistent(directory);
        }
        if (!directory.isDirectory()) {
            throw new IOException("Attempted to access non-directory " + directory);
        }

        final Path tmpPath = tmpPath(directory);
        if (Files.deleteIfExists(tmpPath)) {
            LOG.info("Discarded temporary state file {}", tmpPath);
        }

        final Path binPath = binPath(directory);
        try (DataInputStream in = new DataInputStream(Files.newInputStream(binPath, READ, NOFOLLOW_LINKS))) {
            final int magic = in.readInt();
            if (magic != FILE_MAGIC) {
                throw new IOException("Unrecognized state magic " + Integer.toHexString(magic) + " in " + binPath);
            }
            final int stateVersion = in.readUnsignedByte();
            switch (stateVersion) {
                case STATE_VERSION_ONE:
                    final int state = in.readUnsignedByte();
                    switch (state) {
                        case ST_DIRLAYOUT_ZERO:
                            return new Legacy(directory);
                        case ST_DIRLAYOUT_ONE:
                            return new Current(directory);
                        case ST_DIRLAYOUT_ZERO_ONE_PHASE1:
                            return new FragmentingEntries(directory, in.readLong(), in.readLong());
                        case ST_DIRLAYOUT_ZERO_ONE_PHASE2:
                            return new DeletingDataSegFile(directory);
                        default:
                            throw new IOException("Unrecognized state " + state + " in " + binPath);
                    }
                default:
                    throw new IOException("Unrecognized state version " + stateVersion + " in " + binPath);
            }
        } catch (NoSuchFileException e) {
            LOG.debug("No state file present at {}, assuming legacy version", binPath, e);
            return new Legacy(directory);
        }
    }

    final File directory() {
        return directory;
    }

    final void checkpoint() throws IOException {
        final byte[] bytes = serializeState();
        final Path tmpPath = tmpPath(directory);
        LOG.debug("State {} preparing in {}", this, tmpPath);
        Files.write(tmpPath, bytes, CREATE_NEW, SYNC, NOFOLLOW_LINKS);
        // we have used SYNC, at this point the tmp file is safely written (if it is local, otherwise we do not care)
        LOG.debug("State {} prepared in {}", this, tmpPath);

        final Path binPath = binPath(directory);
        Files.move(tmpPath, binPath, ATOMIC_MOVE);
        // we have used ATOMIC_MOVE, hence the tmp file is now safely the bin file (if it is local)
        LOG.info("State {} checkpointed to {}", this, binPath);
    }

    private byte[] serializeState() throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bos)) {
            out.writeInt(FILE_MAGIC);
            out.writeByte(STATE_VERSION_ONE);
            serializeTo(out);
        }
        return bos.toByteArray();
    }

    private static Path binPath(final File directory) {
        return new File(directory, "state.bin").toPath();
    }

    private static Path tmpPath(final File directory) {
        return new File(directory, "state.tmp").toPath();
    }

    abstract void serializeTo(DataOutput out) throws IOException;

    @Override
    public final String toString() {
        return addToStringAttributes(MoreObjects.toStringHelper(this)).toString();
    }

    ToStringHelper addToStringAttributes(final ToStringHelper helper) {
        // No-op most of the time
        return helper;
    }
}
