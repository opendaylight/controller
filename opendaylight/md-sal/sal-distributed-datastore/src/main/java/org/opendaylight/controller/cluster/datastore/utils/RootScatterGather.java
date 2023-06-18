/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.builder.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.tree.api.DataValidationFailedException;

/**
 * Utility methods for dealing with datastore root {@link ContainerNode} with respect to module shards.
 */
public final class RootScatterGather {
    // FIXME: Record when we have JDK17+
    @NonNullByDefault
    public static final class ShardContainer<T> {
        private final ContainerNode container;
        private final T shard;

        ShardContainer(final T shard, final ContainerNode container) {
            this.shard = requireNonNull(shard);
            this.container = requireNonNull(container);
        }

        public T shard() {
            return shard;
        }

        public ContainerNode container() {
            return container;
        }

        @Override
        public int hashCode() {
            return shard.hashCode();
        }

        @Override
        public boolean equals(final @Nullable Object obj) {
            return obj == this || obj instanceof ShardContainer && shard.equals(((ShardContainer<?>) obj).shard);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("shard", shard).toString();
        }
    }

    private RootScatterGather() {
        // Hidden on purpose
    }

    /**
     * Check whether a {@link NormalizedNode} represents a root container and return it cast to {@link ContainerNode}.
     *
     * @param node a normalized node
     * @return {@code node} cast to ContainerNode
     * @throws NullPointerException if {@code node} is null
     * @throws IllegalArgumentException if {@code node} is not a {@link ContainerNode}
     */
    public static @NonNull ContainerNode castRootNode(final NormalizedNode node) {
        final var nonnull = requireNonNull(node);
        checkArgument(nonnull instanceof ContainerNode, "Invalid root data %s", nonnull);
        return (ContainerNode) nonnull;
    }

    /**
     * Reconstruct root container from a set of constituents.
     *
     * @param actorUtils {@link ActorUtils} reference
     * @param readFutures Consitutent read futures
     * @return A composite future
     */
    public static @NonNull FluentFuture<Optional<NormalizedNode>> gather(final ActorUtils actorUtils,
            final Stream<FluentFuture<Optional<NormalizedNode>>> readFutures) {
        return FluentFuture.from(Futures.transform(
            Futures.allAsList(readFutures.collect(ImmutableList.toImmutableList())), input -> {
                try {
                    return NormalizedNodeAggregator.aggregate(YangInstanceIdentifier.of(), input,
                        actorUtils.getSchemaContext(), actorUtils.getDatastoreContext().getLogicalStoreType());
                } catch (DataValidationFailedException e) {
                    throw new IllegalArgumentException("Failed to aggregate", e);
                }
            }, MoreExecutors.directExecutor()));
    }

    public static <T> @NonNull Stream<ShardContainer<T>> scatterAll(final ContainerNode rootNode,
            final Function<PathArgument, T> childToShard, final Stream<T> allShards) {
        final var builders = allShards
            .collect(Collectors.toUnmodifiableMap(Function.identity(), unused -> Builders.containerBuilder()));
        for (var child : rootNode.body()) {
            final var shard = childToShard.apply(child.name());
            verifyNotNull(builders.get(shard), "Failed to find builder for %s", shard).addChild(child);
        }
        return streamContainers(rootNode.name(), builders);
    }

    /**
     * Split root container into per-shard root containers.
     *
     * @param <T> Shard reference type
     * @param rootNode Root container to be split up
     * @param childToShard Mapping function from child {@link PathArgument} to shard reference
     * @return Stream of {@link ShardContainer}s, one for each touched shard
     */
    public static <T> @NonNull Stream<ShardContainer<T>> scatterTouched(final ContainerNode rootNode,
            final Function<PathArgument, T> childToShard) {
        final var builders = new HashMap<T, DataContainerNodeBuilder<NodeIdentifier, ContainerNode>>();
        for (var child : rootNode.body()) {
            builders.computeIfAbsent(childToShard.apply(child.name()), unused -> Builders.containerBuilder())
                .addChild(child);
        }
        return streamContainers(rootNode.name(), builders);
    }

    private static <T> @NonNull Stream<ShardContainer<T>> streamContainers(final NodeIdentifier rootId,
            final Map<T, DataContainerNodeBuilder<NodeIdentifier, ContainerNode>> builders) {
        return builders.entrySet().stream()
            .map(entry -> new ShardContainer<>(entry.getKey(), entry.getValue().withNodeIdentifier(rootId).build()));
    }
}
