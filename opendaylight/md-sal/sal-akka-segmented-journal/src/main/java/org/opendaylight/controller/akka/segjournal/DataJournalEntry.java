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
 * journal-invariant, but we do store sequenceNr, as it does not match the {@link JournalSegment}'s
 * index if any fragmented entries are present.
 *
 * @author Robert Varga
 */
abstract class DataJournalEntry {

    abstract long getSequenceNr();

    static final class ToPersistence extends DataJournalEntry {
        private final PersistentRepr repr;

        ToPersistence(final PersistentRepr repr) {
            this.repr = requireNonNull(repr);
        }

        PersistentRepr repr() {
            return repr;
        }

        @Override
        long getSequenceNr() {
            return repr.sequenceNr();
        }
    }

    static final class ToFragmentedPersistence extends DataJournalEntry {
        private final PersistentRepr repr;
        private final int fragmentCount;
        private final int fragmentIndex;

        ToFragmentedPersistence(final FragmentedPersistentRepr repr, final int fragmentCount, final int fragmentIndex) {
            this.repr = requireNonNull(repr).toPersistentRepr();
            this.fragmentCount = fragmentCount;
            this.fragmentIndex = fragmentIndex;
        }

        PersistentRepr repr() {
            return repr;
        }

        int getFragmentCount() {
            return fragmentCount;
        }

        int getFragmentIndex() {
            return fragmentIndex;
        }

        @Override
        long getSequenceNr() {
            return repr.sequenceNr();
        }
    }

    static final class FromPersistence extends DataJournalEntry {
        private final long sequenceNr;
        private final String manifest;
        private final String writerUuid;
        private final Object payload;

        FromPersistence(final long sequenceNr, final String manifest, final String writerUuid, final Object payload) {
            this.sequenceNr = sequenceNr;
            this.manifest = manifest;
            this.writerUuid = requireNonNull(writerUuid);
            this.payload = requireNonNull(payload);
        }

        PersistentRepr toRepr(final String persistenceId) {
            return PersistentRepr.apply(payload, sequenceNr, persistenceId, manifest, false, null, writerUuid);
        }

        @Override
        long getSequenceNr() {
            return sequenceNr;
        }
    }

    static final class FromFragmentedPersistence extends DataJournalEntry {
        private final long sequenceNr;
        private final String manifest;
        private final String writerUuid;
        private final int fragmentCount;
        private final int fragmentIndex;
        private final byte[] payload;

        FromFragmentedPersistence(final long sequenceNr, final String manifest, final String writerUuid,
            final int fragmentCount, final int fragmentIndex, final byte[] payload) {
            this.sequenceNr = sequenceNr;
            this.manifest = manifest;
            this.writerUuid = requireNonNull(writerUuid);
            this.fragmentCount = fragmentCount;
            this.fragmentIndex = fragmentIndex;
            this.payload = requireNonNull(payload);
        }

        FragmentedPersistentRepr toRepr(final String persistenceId) {
            return FragmentedPersistentRepr.apply(payload, sequenceNr, persistenceId, manifest, false,
                null, writerUuid);
        }

        int getFragmentIndex() {
            return fragmentIndex;
        }

        int getFragmentCount() {
            return fragmentCount;
        }

        @Override
        long getSequenceNr() {
            return sequenceNr;
        }
    }
}
