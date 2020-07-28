/*
 * Copyright (c) 2020 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.akka.segjournal;

import akka.persistence.PersistentRepr;
import com.google.common.base.Preconditions;
import io.atomix.storage.journal.SegmentedJournalWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.SerializationUtils;
import org.opendaylight.controller.akka.segjournal.DataJournalEntry.ToFragmentedPersistence;

final class DataJournalEntryFragmenter {

    private DataJournalEntryFragmenter() {
    }

    /**
     * Split the payload of the provided {@link PersistentRepr} into fragments the size of maxEntrySize and write them
     * to the journal using the provided writer.
     * @param writer - used to write the fragments into journal
     * @param repr - the original entry which needs to be fragmented
     * @param maxEntrySize - the maximum size of the fragment
     * @return sum of the size of all the fragments written to journal
     */
    static int write(final SegmentedJournalWriter<DataJournalEntry> writer,
        final PersistentRepr repr, final int maxEntrySize) {

        final byte[] payloadBytes = SerializationUtils.serialize((Serializable) repr.payload());
        final long fullReprPayloadLength = payloadBytes.length;
        final int maximumPayloadLength = calculateMaxPayloadFragmentLengthOfRepr(repr, maxEntrySize);
        final int fragCount = (int) Math.ceil((double) fullReprPayloadLength / maximumPayloadLength);
        int appendedSize = 0;

        for (int fragIndex = 0; fragIndex < fragCount; fragIndex++) {
            final int startPosition = fragIndex * maximumPayloadLength;
            final int endPosition = startPosition + maximumPayloadLength;
            final byte[] fragmentBytes = Arrays.copyOfRange(payloadBytes, startPosition,
                (endPosition < payloadBytes.length ? endPosition : payloadBytes.length));
            final FragmentedPersistentRepr fragmentRepr = FragmentedPersistentRepr.applyFromRepr(fragmentBytes, repr);
            appendedSize += writer.append(new ToFragmentedPersistence(fragmentRepr, fragCount, fragIndex)).size();
        }
        return appendedSize;
    }

    /**
     * Get the maximum size of the payload fragment so that together with the header it reaches the maxEntrySize.
     */
    private static int calculateMaxPayloadFragmentLengthOfRepr(final PersistentRepr repr, final int maxEntrySize) {
        return maxEntrySize - getReprHeaderLength(repr);
    }

    private static int getReprHeaderLength(final PersistentRepr repr) {
        Preconditions.checkNotNull(repr, "Repr cannot by null");
        final PersistentRepr emptyPayloadRepr = PersistentRepr.apply(null, repr.sequenceNr(), repr.persistenceId(),
            repr.manifest(), repr.deleted(), repr.sender(), repr.writerUuid());
        return SerializationUtils.serialize(emptyPayloadRepr).length;
    }

    /**
     * Iterate over the list of {@link FragmentedPersistentRepr} extract the payload fragments and merge them together
     * before deserializing into the original {@link PersistentRepr}.
     */
    static PersistentRepr defragmentRepr(final List<FragmentedPersistentRepr> reprFragments) throws IOException {
        Preconditions.checkNotNull(reprFragments, "PayloadFragments cannot be null");
        Preconditions.checkArgument(!reprFragments.isEmpty(), "PayloadFragments cannot be empty");
        Object defragmentedPayload;
        try (ByteArrayOutputStream payloadAggregator = new ByteArrayOutputStream()) {
            for (FragmentedPersistentRepr reprFragment : reprFragments) {
                payloadAggregator.write(reprFragment.payloadFragment());
            }
            byte[] payloadBytes = payloadAggregator.toByteArray();
            defragmentedPayload = SerializationUtils.deserialize(payloadBytes);
        } catch (IOException e) {
            throw new IOException("Defragmentation of an entry failed!", e);
        }
        final FragmentedPersistentRepr firstFragment = reprFragments.get(0);
        return PersistentRepr.apply(defragmentedPayload, firstFragment.sequenceNr(), firstFragment.persistenceId(),
            firstFragment.manifest(), firstFragment.deleted(), firstFragment.sender(), firstFragment.writerUuid());
    }
}
