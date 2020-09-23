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
import io.atomix.storage.journal.JournalSegment;

/**
 * A single entry in the data journal. We do not store {@code persistenceId} for each entry, as that is a
 * journal-invariant, nor do we store {@code sequenceNr}, as that information is maintained by {@link JournalSegment}'s
 * index.
 *
 * @author Robert Varga
 */
abstract class DataJournalEntry {
    /**
     * A single data journal entry on its way to the backing file. This class is used by both Version 0 and Version 1
     * data journal.
     */
    static final class ToPersistence extends DataJournalEntry {
        private final PersistentRepr repr;

        ToPersistence(final PersistentRepr repr) {
            this.repr = requireNonNull(repr);
        }

        PersistentRepr repr() {
            return repr;
        }
    }

    /**
     * A single data journal entry on its way from the backing file. This class is used by both Version 0 and Version 1
     * data journal.
     */
    static final class FromPersistence extends DataJournalEntry {
        private final String manifest;
        private final String writerUuid;
        private final Object payload;

        FromPersistence(final String manifest, final String writerUuid, final Object payload) {
            this.manifest = manifest;
            this.writerUuid = requireNonNull(writerUuid);
            this.payload = requireNonNull(payload);
        }

        PersistentRepr toRepr(final String persistenceId, final long sequenceNr) {
            return PersistentRepr.apply(payload, sequenceNr, persistenceId, manifest, false, null, writerUuid);
        }
    }
}
