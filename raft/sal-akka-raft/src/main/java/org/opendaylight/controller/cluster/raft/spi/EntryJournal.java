/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.raft.api.EntryMeta;
import org.opendaylight.raft.journal.RaftJournal;
import org.opendaylight.raft.spi.CompressionType;
import org.opendaylight.yangtools.concepts.Immutable;

/**
 * Interface for RAFT log entry persistence implementations. This is rather opinionated take with {@link RaftJournal}
 * acting as the backing implementation.
 */
@NonNullByDefault
public interface EntryJournal extends AutoCloseable {
    /**
     * A handle to a {@link LogEntry} stored in the journal. It exposes the entry's {@link EntryMeta} information and
     * provides access to the serialized form of {@link LogEntry#command()} via {@link #openCommandStream()}.
     */
    abstract class JournalEntry implements EntryMeta, Immutable {
        private final long index;
        private final long term;
        private final CompressionType compression;

        protected JournalEntry(long index, long term, CompressionType compression) {
            this.index = index;
            this.term = term;
            this.compression = requireNonNull(compression);
        }

        @Override
        public final long index() {
            return index;
        }

        @Override
        public final long term() {
            return term;
        }

        /**
         * Returns an {@link InputStream} containing this entry's command in its serialized form.
         *
         * @return an {@link InputStream}
         * @throws IOException if an I/O error occurs
         */
        public final InputStream openCommandStream() throws IOException {
            return compression.decodeInput(verifyNotNull(newCommandStream()));
        }

        /**
         * Open a new {@link InputStream} containing this entry's command in its serialized form as stored
         * in the journal, i.e. compressed with {@link #compression}. The returned stream is assumed to buffered.
         *
         * @return a buffered {@link InputStream}
         * @throws IOException if an I/O error occurs
         */
        protected abstract InputStream newCommandStream() throws IOException;

        /**
         * Convert this {@link JournalEntry} into a {@link LogEntry} by interpreting {@link #openCommandStream()} as
         * a stream comtaining {@link LogEntry#command()} serialized via Java serialization of its
         * {@link StateMachineCommand#toSerialForm()}.
         *
         * @return the {@link LogEntry} equivalent
         * @throws IOException if an I/O error occurs or the corresponding class cannot be resolver
         */
        public final LogEntry toLogEntry() throws IOException {
            final StateMachineCommand command;
            try (var ois = new ObjectInputStream(openCommandStream())) {
                try {
                    command = requireNonNull((StateMachineCommand) ois.readObject());
                } catch (ClassNotFoundException e) {
                    throw new IOException("Cannot resolve command class ", e);
                }
            }
            return new DefaultLogEntry(index, term, command);
        }

        @Override
        public final String toString() {
            return addToStringAttributes(MoreObjects.toStringHelper(this).add("index", index()).add("term", term))
                .toString();
        }

        /**
         * Enrich a {@link ToStringHelper} with additional attributes beyond {@link #index()} and {@link #term()}.
         *
         * @param helper the {@link ToStringHelper}
         * @return the {@link ToStringHelper}
         */
        protected abstract ToStringHelper addToStringAttributes(ToStringHelper helper);
    }

    /**
     * A reader-side interface to log entries being stored in this journal.
     */
    interface Reader extends AutoCloseable {
        /**
         * {@return Returns the {@code journalIndex}, guaranteed to be positive}
         */
        long nextJournalIndex();

        /**
         * {@return the next {@link JournalEntry}, or {@code null}}
         * @throws IOException if an I/O error occurs
         */
        @Nullable JournalEntry nextEntry() throws IOException;

        /**
         * Reset this reader so it attempts to read the entry at specified {@code journalIndex}.
         *
         * @param nextJournalIndex the next {@code journalIndex} to read, must be positive
         * @throws IOException if an I/O error occurs
         */
        void resetToRead(long nextJournalIndex) throws IOException;

        @Override
        void close();
    }

    /**
     * A friendly constant to centralize the places where we assume counting from 1.
     */
    int FIRST_JOURNAL_INDEX = 1;

    /**
     * {@return the {@code journalIndex} of the last entry which is to be applied}
     */
    // FIXME: commitJournalIndex()?
    long applyToJournalIndex();

    /**
     * {@return a new {@link Reader}}
     */
    Reader openReader();

    /**
     * Append an entry at the current {@code journalIndex}.
     *
     * @param entry the journal entry to append
     * @return the serialized size of {@link LogEntry#command()}
     * @throws IOException if an I/O error occurs
     */
    long appendEntry(LogEntry entry) throws IOException;

    /**
     * Discard all entries starting from the beginning of journal up to, but excluding, {@code firstRetainedIndex}. The
     * journal will be updated such that the next {@link #openReader()} invocation will result in the reader reporting
     * {@code firstRetainedIndex} as the initial {@link Reader#nextJournalIndex()}.
     *
     * @param firstRetainedIndex the index of the first entry to retain
     * @throws IOException if an I/O error occurs
     */
    void discardHead(long firstRetainedIndex) throws IOException;

    /**
     * Discard all entries starting from {@code firstRemovedIndex}. The journal will be positioned such that the next
     * {@link #appendEntry(LogEntry)} will return {@code firstRemovedIndex}.
     *
     * @param firstRemovedIndex the index of the first entry to remove
     * @throws IOException if an I/O error occurs
     */
    void discardTail(long firstRemovedIndex) throws IOException;

    /**
     * Update the {@code journalIndex} to report from {@link #applyToJournalIndex()}.
     *
     * @param newApplyTo the new {@code journalIndex} to return
     * @throws IOException if an I/O error occurs
     */
    void setApplyTo(long newApplyTo) throws IOException;

    @Override
    void close();
}
