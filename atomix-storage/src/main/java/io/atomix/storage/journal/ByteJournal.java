/*
 * Copyright (c) 2024 PANTHEON.tech s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.storage.journal;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A journal of byte arrays. Provides the ability to write modify entries via {@link ByteJournalWriter} and read them
 * back via {@link ByteJournalReader}.
 */
@NonNullByDefault
public interface ByteJournal extends AutoCloseable {
    /**
     * Returns the journal writer.
     *
     * @return The journal writer.
     */
    ByteJournalWriter writer();

    /**
     * Opens a new {@link ByteJournalReader} reading all entries.
     *
     * @param index The index at which to start the reader.
     * @return A new journal reader.
     */
    ByteJournalReader openReader(long index);

    /**
     * Opens a new {@link ByteJournalReader} reading only committed entries.
     *
     * @param index The index at which to start the reader.
     * @return A new journal reader.
     */
    ByteJournalReader openCommitsReader(long index);

    @Override
    void close();
}
