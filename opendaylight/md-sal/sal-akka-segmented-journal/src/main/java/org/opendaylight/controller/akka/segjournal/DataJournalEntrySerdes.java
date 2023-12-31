/*
 * Copyright (c) 2019 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.akka.segjournal;

import static java.util.Objects.requireNonNull;

import com.google.common.base.VerifyException;
import io.atomix.storage.journal.JournalSerdes.EntryInput;
import io.atomix.storage.journal.JournalSerdes.EntryOutput;
import io.atomix.storage.journal.JournalSerdes.EntrySerdes;
import java.io.IOException;
import java.util.concurrent.Callable;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.ExtendedActorSystem;
import org.apache.pekko.persistence.PersistentRepr;
import org.apache.pekko.serialization.JavaSerializer;
import org.opendaylight.controller.akka.segjournal.DataJournalEntry.FromPersistence;
import org.opendaylight.controller.akka.segjournal.DataJournalEntry.ToPersistence;

/**
 * Kryo serializer for {@link DataJournalEntry}. Each {@link SegmentedJournalActor} has its own instance, as well as
 * a nested JavaSerializer to handle the payload.
 *
 * <p>Since we are persisting only parts of {@link PersistentRepr}, this class asymmetric by design:
 * {@link #write(EntryOutput, DataJournalEntry)} only accepts {@link ToPersistence} subclass, which is a wrapper
 * around a {@link PersistentRepr}, while {@link #read(EntryInput)} produces an {@link FromPersistence}, which
 * needs further processing to reconstruct a {@link PersistentRepr}.
 */
final class DataJournalEntrySerdes implements EntrySerdes<DataJournalEntry> {
    private final ExtendedActorSystem actorSystem;

    DataJournalEntrySerdes(final ActorSystem actorSystem) {
        this.actorSystem = requireNonNull((ExtendedActorSystem) actorSystem);
    }

    @Override
    public void write(final EntryOutput output, final DataJournalEntry entry) throws IOException {
        if (entry instanceof ToPersistence toPersistence) {
            output.writeString(toPersistence.manifest());
            output.writeString(toPersistence.writerUuid());
            output.writeObject(toPersistence.payload());
        } else {
            throw new VerifyException("Unexpected entry " + entry);
        }
    }

    @Override
    public DataJournalEntry read(final EntryInput input) throws IOException {
        return new FromPersistence(input.readString(), input.readString(),
            JavaSerializer.currentSystem().withValue(actorSystem, (Callable<Object>) input::readObject));
    }
}
