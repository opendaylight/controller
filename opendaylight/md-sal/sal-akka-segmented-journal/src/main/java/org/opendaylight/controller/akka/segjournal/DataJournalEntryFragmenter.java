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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DataJournalEntryFragmenter {
    private static final Logger LOG = LoggerFactory.getLogger(DataJournalEntryFragmenter.class);

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

    private static int calculateMaxPayloadFragmentLengthOfRepr(final PersistentRepr repr, final int maxEntrySize) {
        return maxEntrySize - getReprHeaderLength(repr);
    }

    private static int getReprHeaderLength(final PersistentRepr repr) {
        Preconditions.checkNotNull(repr, "Repr can't by null");
        final PersistentRepr emptyPayloadRepr = PersistentRepr.apply(null, repr.sequenceNr(), repr.persistenceId(),
            repr.manifest(), repr.deleted(), repr.sender(), repr.writerUuid());
        return SerializationUtils.serialize(emptyPayloadRepr).length;
    }

    static PersistentRepr defragmentRepr(final List<FragmentedPersistentRepr> reprFragments) {
        Preconditions.checkNotNull(reprFragments, "PayloadFragments can't be null");
        Preconditions.checkArgument(!reprFragments.isEmpty(), "PayloadFragments can't be empty");
        Object defragmentedPayload = null;
        try (ByteArrayOutputStream payloadAggregator = new ByteArrayOutputStream()) {
            for (FragmentedPersistentRepr reprFragment : reprFragments) {
                payloadAggregator.write(reprFragment.payloadFragment());
            }
            byte[] payloadBytes = payloadAggregator.toByteArray();
            defragmentedPayload = SerializationUtils.deserialize(payloadBytes);
        } catch (IOException e) {
            LOG.error("Defragmentation of PersistentRepr failed.", e);
        }
        final FragmentedPersistentRepr firstFragment = reprFragments.get(0);
        return PersistentRepr.apply(defragmentedPayload, firstFragment.sequenceNr(), firstFragment.persistenceId(),
            firstFragment.manifest(), firstFragment.deleted(), firstFragment.sender(), firstFragment.writerUuid());
    }
}
