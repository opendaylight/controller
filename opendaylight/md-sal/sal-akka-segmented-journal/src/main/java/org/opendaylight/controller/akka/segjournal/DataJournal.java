/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.akka.segjournal;

import static java.util.Objects.requireNonNull;

import com.codahale.metrics.Histogram;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.akka.segjournal.SegmentedJournalActor.ReplayMessages;
import org.opendaylight.controller.akka.segjournal.SegmentedJournalActor.WriteMessages;

/**
 * Abstraction of a data journal. This provides a unified interface towards {@link SegmentedJournalActor}, allowing
 * specialization for various formats.
 */
abstract class DataJournal {
    // Mirrors fields from associated actor
    final @NonNull String persistenceId;
    private final Histogram messageSize;

    // Tracks largest message size we have observed either during recovery or during write
    private int largestObservedSize;

    DataJournal(final String persistenceId, final Histogram messageSize) {
        this.persistenceId = requireNonNull(persistenceId);
        this.messageSize = requireNonNull(messageSize);
    }

    final void recordMessageSize(final int size) {
        messageSize.update(size);
        updateLargestSize(size);
    }

    final void updateLargestSize(final int size) {
        if (size > largestObservedSize) {
            largestObservedSize = size;
        }
    }

    abstract long lastWrittenIndex();

    abstract void commitTo(long index);

    abstract void compactTo(long index);

    abstract void close();

    abstract void handleReplayMessages(ReplayMessages message, long from);

    abstract void handleWriteMessages(WriteMessages message);
}
