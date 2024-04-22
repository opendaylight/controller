/*
 * Copyright (c) 2024 PANTHEON.tech s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.storage.journal;

import io.netty.buffer.ByteBuf;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Byte journal reader.
 */
public interface ByteJournalReader extends AutoCloseable {

    /**
     * Returns the first index in the journal.
     *
     * @return The first index in the journal
     */
    long firstIndex();

    /**
     * Returns the last read index.
     *
     * @return The last read index
     */
    long lastIndex();

    /**
     * Returns the next reader index.
     *
     * @return The next reader index
     */
    long nextIndex();

    /**
     * Try to move to the next binary data block
     *
     * @param mapper callback to be invoked on binary data
     * @return processed binary data, or {@code null}
     */
    <T> @Nullable T tryNext(JournalReader.EntryMapper<ByteBuf, T> mapper);

    /**
     * Resets the reader to the start.
     */
    void reset();

    /**
     * Resets the reader to the given index.
     *
     * @param index The index to which to reset the reader
     */
    void reset(long index);

    @Override
    void close();
}
