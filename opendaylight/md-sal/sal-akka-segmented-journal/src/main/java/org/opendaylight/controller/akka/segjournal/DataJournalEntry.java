/*
 * Copyright (c) 2019 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.akka.segjournal;

import static java.util.Objects.requireNonNull;

import org.apache.pekko.persistence.PersistentRepr;

/**
 * A single entry in the data journal. We do not store {@code persistenceId} for each entry, as that is a
 * journal-invariant, nor do we store {@code sequenceNr}, as that information is maintained by a particular journal
 * segment's index.
 */
abstract sealed class DataJournalEntry {
    /**
     * A single data journal entry on its way to the backing file.
     */
    static final class ToPersistence extends DataJournalEntry {
        private final String manifest;
        private final String writerUuid;
        private final Object payload;

        ToPersistence(final String manifest, final String writerUuid, final Object payload) {
            this.manifest = manifest;
            this.writerUuid = writerUuid;
            this.payload = requireNonNull(payload);
        }

        String manifest() {
            return manifest;
        }

        String writerUuid() {
            return writerUuid;
        }

        Object payload() {
            return payload;
        }
    }

    /**
     * A single data journal entry on its way from the backing file.
     */
    static final class FromPersistence extends DataJournalEntry {
        private final String manifest;
        private final String writerUuid;
        private final Object payload;

        FromPersistence(final String manifest, final String writerUuid, final Object payload) {
            this.manifest = manifest;
            this.writerUuid = writerUuid;
            this.payload = requireNonNull(payload);
        }

        String writerUuid() {
            return writerUuid;
        }

        PersistentRepr toRepr(final String persistenceId, final long sequenceNr) {
            requireNonNull(writerUuid);
            return toRepr(persistenceId, sequenceNr, writerUuid);
        }

        PersistentRepr toRepr(final String persistenceId, final long sequenceNr, final String reprWriterUuid) {
            return PersistentRepr.apply(payload, sequenceNr, persistenceId, manifest, false, null, reprWriterUuid);
        }
    }
}
