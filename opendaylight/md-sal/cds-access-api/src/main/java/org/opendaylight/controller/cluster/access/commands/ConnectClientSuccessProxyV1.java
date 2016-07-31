/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.serialization.JavaSerializer;
import akka.serialization.Serialization;
import java.io.DataInput;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.opendaylight.controller.cluster.access.concepts.AbstractSuccessProxy;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;

/**
 * Externalizable proxy for use with {@link ConnectClientSuccess}. It implements the initial (Boron) serialization
 * format.
 *
 * @author Robert Varga
 */
final class ConnectClientSuccessProxyV1 extends AbstractSuccessProxy<ClientIdentifier, ConnectClientSuccess> {
    private static final long serialVersionUID = 1L;

    private List<ActorSelection> alternates;
    private ActorRef backend;

    public ConnectClientSuccessProxyV1() {
        // For Externalizable
    }

    ConnectClientSuccessProxyV1(final ConnectClientSuccess success) {
        super(success);
        this.alternates = success.getAlternates();
        this.backend = success.getBackend();
        // We are ignoring the DataTree, it is not serializable anyway
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        super.writeExternal(out);

        out.writeUTF(Serialization.serializedActorPath(backend));

        out.writeInt(alternates.size());
        for (ActorSelection b : alternates) {
            out.writeObject(b.toSerializationFormat());
        }
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);

        backend = JavaSerializer.currentSystem().value().provider().resolveActorRef(in.readUTF());

        final int backendsSize = in.readInt();
        if (backendsSize < 1) {
            throw new IOException("Illegal number of backends " + backendsSize);
        }

        alternates = new ArrayList<>(backendsSize);
        for (int i = 0; i < backendsSize; ++i) {
            alternates.add(ActorSelection.apply(ActorRef.noSender(), (String)in.readObject()));
        }
    }

    @Override
    protected ConnectClientSuccess createSuccess(final ClientIdentifier target) {
        return new ConnectClientSuccess(target, backend, alternates, Optional.empty());
    }

    @Override
    protected ClientIdentifier readTarget(final DataInput in) throws IOException {
        return ClientIdentifier.readFrom(in);
    }
}
