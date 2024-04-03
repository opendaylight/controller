/*
 * Copyright (c) 2024 PANTHEON.tech s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.storage.journal;

import java.io.Closeable;

/**
 * Byte Journal.
 */
public interface ByteJournal extends Closeable {

    /**
     * Returns the journal writer.
     *
     * @return The journal writer.
     */
    ByteJournalWriter writer();

    /**
     * Opens a new journal reader.
     *
     * @param index The index at which to start the reader.
     * @return A new journal reader.
     */
    ByteJournalReader newReader(long index);

    /**
     * Opens a new journal reader.
     *
     * @param index The index at which to start the reader.
     * @param mode the reader mode
     * @return A new journal reader.
     */
    ByteJournalReader newReader(long index, JournalReader.Mode mode);

    /**
     * Returns a boolean indicating whether the journal is open.
     *
     * @return Indicates whether the journal is open.
     */
    boolean isOpen();
}
