/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.akka.segjournal;

import akka.actor.ActorSystem;
import akka.persistence.PersistentRepr;
import com.codahale.metrics.Histogram;
import io.atomix.storage.journal.Indexed;
import io.atomix.storage.journal.SegmentedJournalWriter;
import io.atomix.storage.journal.StorageLevel;
import java.io.File;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.akka.segjournal.DataJournalEntry.FromPersistence;
import org.opendaylight.controller.akka.segjournal.DataJournalEntry.ToPersistence;

/**
 * Version 1 data journal: writerUuid is tracked in separate state.
 */
final class DataJournalV1 extends DataJournalV0 {
    DataJournalV1(final String persistenceId, final Histogram messageSize, final ActorSystem system,
            final StorageLevel storage, final File directory, final int maxEntrySize, final int maxSegmentSize) {
        super(persistenceId, messageSize, system, storage, directory, maxEntrySize, maxSegmentSize);
        // FIXME: additional file :(
    }

    @Override
    PersistentRepr toRepr(final FromPersistence entry, final long sequenceNr) {
        // FIXME: lookup writerUuid for sequence nr
        final String writerUuid = null;
        return entry.toRepr(persistenceId, sequenceNr, writerUuid);
    }

    @Override
    Indexed<@NonNull ToPersistence> writerRepr(final SegmentedJournalWriter<DataJournalEntry> writer,
            final PersistentRepr repr) {
        final var ret = writer.append(new ToPersistence(repr.manifest(), null, repr.payload()));
        final var writerUuid = repr.writerUuid();

        // FIXME: check if writerUuid changed

        return ret;
    }
}
