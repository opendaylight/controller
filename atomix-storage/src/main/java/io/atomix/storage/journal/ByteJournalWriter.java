/*
 * Copyright (c) 2024 PANTHEON.tech s.r.o. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.storage.journal;

/**
 * Byte journal writer.
 */
public interface ByteJournalWriter {
    /**
     * Returns the last written index.
     *
     * @return The last written index
     */
    long lastIndex();

    /**
     * Returns the next index to be written.
     *
     * @return The next index to be written
     */
    long nextIndex();

    /**
     * Returns the last entry written.
     *
     * @return The last entry written
     */
    byte[] lastWritten();

    /**
     * Appends an entry to the journal.
     *
     * @param bytes Data block to append
     * @return The index of appended data block
     */
    long append(byte[] bytes);

    /**
     * Commits entries up to the given index.
     *
     * @param index The index up to which to commit entries.
     */
    void commit(long index);

    /**
     * Resets the head of the journal to the given index.
     *
     * @param index The index to which to reset the head of the journal
     */
    void reset(long index);

    /**
     * Truncates the log to the given index.
     *
     * @param index The index to which to truncate the log.
     */
    void truncate(long index);

    /**
     * Flushes written entries to disk.
     */
    void flush();
}
