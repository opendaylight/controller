/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.Collection;
import java.util.Optional;
import org.apache.pekko.actor.ActorRef;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.remote.rpc.registry.gossip.BucketData;

/**
 * Common class for routing tables.
 *
 * @param <T> Table type
 * @param <I> Item type
 */
public abstract class AbstractRoutingTable<T extends AbstractRoutingTable<T, I>, I> implements BucketData<T>,
        Serializable {
    private static final long serialVersionUID = 1L;

    private final @NonNull ActorRef invoker;
    private final @NonNull ImmutableSet<I> items;

    AbstractRoutingTable(final ActorRef invoker, final Collection<I> items) {
        this.invoker = requireNonNull(invoker);
        this.items = ImmutableSet.copyOf(items);
    }

    @Override
    public final Optional<ActorRef> getWatchActor() {
        return Optional.of(invoker);
    }

    public final @NonNull ImmutableSet<I> getItems() {
        return items;
    }

    final @NonNull ActorRef getInvoker() {
        return invoker;
    }

    @VisibleForTesting
    public final boolean contains(final I routeId) {
        return items.contains(routeId);
    }

    @VisibleForTesting
    public final int size() {
        return items.size();
    }

    abstract Object writeReplace();

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this).add("invoker", invoker).add("items", items).toString();
    }
}
