/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.serialization.JavaSerializer;
import org.apache.pekko.serialization.Serialization;
import org.opendaylight.mdsal.dom.api.DOMRpcIdentifier;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataInput;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataOutput;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeStreamVersion;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;

public final class RoutingTable extends AbstractRoutingTable<RoutingTable, DOMRpcIdentifier> {
    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        private Collection<DOMRpcIdentifier> rpcs;
        private ActorRef opsInvoker;

        // checkstyle flags the public modifier as redundant however it is explicitly needed for Java serialization to
        // be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
            // For Externalizable
        }

        Proxy(final RoutingTable table) {
            rpcs = table.getItems();
            opsInvoker = table.getInvoker();
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeObject(Serialization.serializedActorPath(opsInvoker));

            try (NormalizedNodeDataOutput nnout = NormalizedNodeStreamVersion.current().newDataOutput(out)) {
                nnout.writeInt(rpcs.size());
                for (DOMRpcIdentifier id : rpcs) {
                    // TODO: we should be able to get by with just a QName
                    nnout.writeSchemaNodeIdentifier(Absolute.of(id.getType()));
                    nnout.writeYangInstanceIdentifier(id.getContextReference());
                }
            }
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            opsInvoker = JavaSerializer.currentSystem().value().provider().resolveActorRef((String) in.readObject());

            final NormalizedNodeDataInput nnin = NormalizedNodeDataInput.newDataInput(in);
            final int size = nnin.readInt();
            rpcs = new ArrayList<>(size);
            for (int i = 0; i < size; ++i) {
                // TODO: we should be able to get by with just a QName
                rpcs.add(DOMRpcIdentifier.create(nnin.readSchemaNodeIdentifier().firstNodeIdentifier(),
                    nnin.readYangInstanceIdentifier()));
            }
        }

        private Object readResolve() {
            return new RoutingTable(opsInvoker, rpcs);
        }
    }

    private static final long serialVersionUID = 1L;

    RoutingTable(final ActorRef invoker, final Collection<DOMRpcIdentifier> table) {
        super(invoker, table);
    }

    RoutingTable addRpcs(final Collection<DOMRpcIdentifier> toAdd) {
        final Set<DOMRpcIdentifier> newRpcs = new HashSet<>(getItems());
        newRpcs.addAll(toAdd);
        return new RoutingTable(getInvoker(), newRpcs);
    }

    RoutingTable removeRpcs(final Collection<DOMRpcIdentifier> toRemove) {
        final Set<DOMRpcIdentifier> newRpcs = new HashSet<>(getItems());
        newRpcs.removeAll(toRemove);
        return new RoutingTable(getInvoker(), newRpcs);
    }

    @Override
    Object writeReplace() {
        return new Proxy(this);
    }
}
