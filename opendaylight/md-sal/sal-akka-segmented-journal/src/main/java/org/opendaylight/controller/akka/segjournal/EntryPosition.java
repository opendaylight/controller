/*
 * Copyright (c) 2020 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.akka.segjournal;

import static java.util.Objects.requireNonNull;

import io.atomix.storage.journal.Indexed;
import org.opendaylight.controller.akka.segjournal.DataJournalEntry.FromFragmentedPersistence;
import org.opendaylight.controller.akka.segjournal.DataJournalEntry.ToFragmentedPersistence;

class EntryPosition {

    private final long firstIndex;
    private final long lastIndex;

    EntryPosition(long firstIndex, long lastIndex) {
        this.firstIndex = firstIndex;
        this.lastIndex = lastIndex;
    }

    long getFirstIndex() {
        return firstIndex;
    }

    long getLastIndex() {
        return lastIndex;
    }

    static EntryPosition forEntry(final Indexed<DataJournalEntry> indexedEntry) {
        requireNonNull(indexedEntry, "Entry cannot be null");
        final DataJournalEntry entry = indexedEntry.entry();
        if (entry instanceof FromFragmentedPersistence) {
            final FromFragmentedPersistence fragEntry = (FromFragmentedPersistence)entry;
            return new EntryPosition(indexedEntry.index() - fragEntry.getFragmentIndex(),
                indexedEntry.index() + fragEntry.getFragmentCount() - (fragEntry.getFragmentIndex() + 1));
        } else if (entry instanceof ToFragmentedPersistence) {
            final ToFragmentedPersistence fragEntry = (ToFragmentedPersistence)entry;
            return new EntryPosition(indexedEntry.index() - fragEntry.getFragmentIndex(),
                indexedEntry.index() + fragEntry.getFragmentCount() - (fragEntry.getFragmentIndex() + 1));
        } else {
            return new EntryPosition(indexedEntry.index(), indexedEntry.index());
        }
    }
}
