/*
 * Copyright (c) 2019 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.akka.segjournal;

import static com.google.common.base.Verify.verify;
import static java.util.Objects.requireNonNull;

import akka.actor.ActorSystem;
import akka.actor.ExtendedActorSystem;
import akka.persistence.PersistentRepr;
import io.atomix.storage.journal.JournalSerdes.EntryInput;
import io.atomix.storage.journal.JournalSerdes.EntryOutput;
import io.atomix.storage.journal.JournalSerdes.EntrySerdes;
import java.io.IOException;
import java.util.concurrent.Callable;
import org.opendaylight.controller.akka.segjournal.DataJournalEntry.FromPersistence;
import org.opendaylight.controller.akka.segjournal.DataJournalEntry.ToPersistence;

/**
 * Kryo serializer for {@link DataJournalEntry}. Each {@link SegmentedJournalActor} has its own instance, as well as
 * a nested JavaSerializer to handle the payload.
 *
 * <p>
 * Since we are persisting only parts of {@link PersistentRepr}, this class asymmetric by design:
 * {@link #write(EntryOutput, DataJournalEntry)} only accepts {@link ToPersistence} subclass, which is a wrapper
 * around a {@link PersistentRepr}, while {@link #read(EntryInput)} produces an {@link FromPersistence}, which
 * needs further processing to reconstruct a {@link PersistentRepr}.
 *
 * @author Robert Varga
 */
final class DataJournalEntrySerializer implements EntrySerdes<DataJournalEntry> {
    private final ExtendedActorSystem actorSystem;

    DataJournalEntrySerializer(final ActorSystem actorSystem) {
        this.actorSystem = requireNonNull((ExtendedActorSystem) actorSystem);
    }

    @Override
    public void write(final EntryOutput output, final Object entry) throws IOException {
        verify(entry instanceof ToPersistence);
        final PersistentRepr repr = ((ToPersistence) entry).repr();
        output.writeString(repr.manifest());
        output.writeString(repr.writerUuid());
        output.writeObject(repr.payload());
    }

    @Override
    public DataJournalEntry read(final EntryInput input) throws IOException {
        final String manifest = input.readString();
        final String uuid = input.readString();
        final Object payload = akka.serialization.JavaSerializer.currentSystem().withValue(actorSystem,
            (Callable<Object>) input::readObject);
        return new FromPersistence(manifest, uuid, payload);
    }
}
