/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry;

import akka.actor.ActorRef;
import akka.serialization.JavaSerializer;
import akka.serialization.Serialization;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeDataOutput;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeInputOutput;
import org.opendaylight.mdsal.dom.api.DOMRpcIdentifier;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataInput;

public final class RoutingTable extends AbstractRoutingTable<RoutingTable, DOMRpcIdentifier> {
    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "We deal with the field in serialization methods.")
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

            final NormalizedNodeDataOutput nnout = NormalizedNodeInputOutput.newDataOutput(out);
            nnout.writeInt(rpcs.size());
            for (DOMRpcIdentifier id : rpcs) {
                nnout.writeSchemaPath(id.getType());
                nnout.writeYangInstanceIdentifier(id.getContextReference());
            }
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            opsInvoker = JavaSerializer.currentSystem().value().provider().resolveActorRef((String) in.readObject());

            final NormalizedNodeDataInput nnin = NormalizedNodeDataInput.newDataInput(in);
            final int size = nnin.readInt();
            rpcs = new ArrayList<>(size);
            for (int i = 0; i < size; ++i) {
                rpcs.add(DOMRpcIdentifier.create(nnin.readSchemaPath(), nnin.readYangInstanceIdentifier()));
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
