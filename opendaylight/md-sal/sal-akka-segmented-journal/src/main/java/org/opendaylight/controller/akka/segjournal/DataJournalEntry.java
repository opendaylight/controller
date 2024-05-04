/*
 * Copyright (c) 2019 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.akka.segjournal;

import static java.util.Objects.requireNonNull;

import akka.persistence.PersistentRepr;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.PersistentData;

/**
 * A single entry in the data journal. We do not store {@code persistenceId} for each entry, as that is a
 * journal-invariant, nor do we store {@code sequenceNr}, as that information is maintained by a particular journal
 * segment's index.
 */
@NonNullByDefault
sealed interface DataJournalEntry {
    /**
     * A single data journal entry on its way to the backing file.
     */
    record ToPersistence(PersistentRepr repr) implements DataJournalEntry {
        public ToPersistence {
            requireNonNull(repr);
        }
    }

    /**
     * A single data journal entry on its way from the backing file.
     */
    record FromPersistence(@Nullable String manifest, String writerUuid, PersistentData payload)
            implements DataJournalEntry {
        public FromPersistence {
            requireNonNull(writerUuid);
            requireNonNull(payload);
        }

        PersistentRepr toRepr(final String persistenceId, final long sequenceNr) {
            return PersistentRepr.apply(payload, sequenceNr, persistenceId, manifest, false, null, writerUuid);
        }
    }
}
