/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry;

import akka.actor.ActorRef;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.remote.rpc.registry.gossip.BucketData;

public class RoutingTable implements BucketData<RoutingTable>, Serializable {
    private static final long serialVersionUID = 1L;

    private final Set<DOMRpcIdentifier> rpcs;
    private final ActorRef rpcInvoker;

    private RoutingTable(final ActorRef rpcInvoker, final Set<DOMRpcIdentifier> table) {
        this.rpcInvoker = Preconditions.checkNotNull(rpcInvoker);
        this.rpcs = ImmutableSet.copyOf(table);
    }

    RoutingTable(final ActorRef rpcInvoker) {
        this(rpcInvoker, ImmutableSet.of());
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
