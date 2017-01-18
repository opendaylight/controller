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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeDataInput;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeDataOutput;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeInputOutput;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.remote.rpc.registry.gossip.BucketData;

public final class RoutingTable implements BucketData<RoutingTable>, Serializable {
    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "We deal with the field in serialization methods.")
        private Collection<DOMRpcIdentifier> rpcs;
        private ActorRef rpcInvoker;

        // checkstyle flags the public modifier as redundant however it is explicitly needed for Java serialization to
        // be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
            // For Externalizable
        }

        Proxy(final RoutingTable table) {
            rpcs = table.getRoutes();
            rpcInvoker = table.getRpcInvoker();
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeObject(Serialization.serializedActorPath(rpcInvoker));

            final NormalizedNodeDataOutput nnout = NormalizedNodeInputOutput.newDataOutput(out);
            nnout.writeInt(rpcs.size());
            for (DOMRpcIdentifier id : rpcs) {
                nnout.writeSchemaPath(id.getType());
                nnout.writeYangInstanceIdentifier(id.getContextReference());
            }
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            rpcInvoker = JavaSerializer.currentSystem().value().provider().resolveActorRef((String) in.readObject());

            final NormalizedNodeDataInput nnin = NormalizedNodeInputOutput.newDataInput(in);
            final int size = nnin.readInt();
            rpcs = new ArrayList<>(size);
            for (int i = 0; i < size; ++i) {
                rpcs.add(DOMRpcIdentifier.create(nnin.readSchemaPath(), nnin.readYangInstanceIdentifier()));
            }
        }

        private Object readResolve() {
            return new RoutingTable(rpcInvoker, rpcs);
        }
    }

    private static final long serialVersionUID = 1L;

    @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "We deal with the field in serialization methods.")
    private final Set<DOMRpcIdentifier> rpcs;
    private final ActorRef rpcInvoker;

    RoutingTable(final ActorRef rpcInvoker, final Collection<DOMRpcIdentifier> table) {
        this.rpcInvoker = Preconditions.checkNotNull(rpcInvoker);
        this.rpcs = ImmutableSet.copyOf(table);
    }

    @Override
    public Optional<ActorRef> getWatchActor() {
        return Optional.of(rpcInvoker);
    }

    public Set<DOMRpcIdentifier> getRoutes() {
        return rpcs;
    }

    ActorRef getRpcInvoker() {
        return rpcInvoker;
    }

    RoutingTable addRpcs(final Collection<DOMRpcIdentifier> toAdd) {
        final Set<DOMRpcIdentifier> newRpcs = new HashSet<>(rpcs);
        newRpcs.addAll(toAdd);
        return new RoutingTable(rpcInvoker, newRpcs);
    }

    RoutingTable removeRpcs(final Collection<DOMRpcIdentifier> toRemove) {
        final Set<DOMRpcIdentifier> newRpcs = new HashSet<>(rpcs);
        newRpcs.removeAll(toRemove);
        return new RoutingTable(rpcInvoker, newRpcs);
    }

    private Object writeReplace() {
        return new Proxy(this);
    }

    @VisibleForTesting
    boolean contains(final DOMRpcIdentifier routeId) {
        return rpcs.contains(routeId);
    }

    @VisibleForTesting
    int size() {
        return rpcs.size();
    }

    @Override
    public String toString() {
        return "RoutingTable{" + "rpcs=" + rpcs + ", rpcInvoker=" + rpcInvoker + '}';
    }
}
