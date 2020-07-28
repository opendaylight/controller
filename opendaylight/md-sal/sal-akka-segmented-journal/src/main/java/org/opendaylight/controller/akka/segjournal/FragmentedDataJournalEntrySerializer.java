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
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import java.util.concurrent.Callable;
import org.opendaylight.controller.akka.segjournal.DataJournalEntry.FromFragmentedPersistence;
import org.opendaylight.controller.akka.segjournal.DataJournalEntry.ToFragmentedPersistence;

final class FragmentedDataJournalEntrySerializer extends Serializer<DataJournalEntry> {
    private final JavaSerializer serializer = new JavaSerializer();
    private final ExtendedActorSystem actorSystem;

    FragmentedDataJournalEntrySerializer(final ActorSystem actorSystem) {
        this.actorSystem = requireNonNull((ExtendedActorSystem) actorSystem);
    }

    @Override
    public void write(final Kryo kryo, final Output output, final DataJournalEntry object) {
        verify(object instanceof ToFragmentedPersistence);
        final ToFragmentedPersistence fragmentedPersistence = (ToFragmentedPersistence) object;
        final PersistentRepr repr = fragmentedPersistence.repr();
        output.writeLong(fragmentedPersistence.getSequenceNr());
        output.writeString(repr.manifest());
        output.writeString(repr.writerUuid());
        output.writeInt(fragmentedPersistence.getFragmentCount());
        output.writeInt(fragmentedPersistence.getFragmentIndex());
        serializer.write(kryo, output, repr.payload());
    }

    @Override
    public DataJournalEntry read(final Kryo kryo, final Input input, final Class<DataJournalEntry> type) {
        final long sequenceNr = input.readLong();
        final String manifest = input.readString();
        final String uuid = input.readString();
        final int fragmentCount = input.readInt();
        final int fragmentIndex = input.readInt();
        final byte[] payload = akka.serialization.JavaSerializer.currentSystem().withValue(actorSystem,
            (Callable<byte[]>) () -> (byte[]) serializer.read(kryo, input, type));
        return new FromFragmentedPersistence(sequenceNr, manifest, uuid, fragmentCount, fragmentIndex, payload);
    }
}
