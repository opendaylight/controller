/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.sharding;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.spi.shard.ForeignShardModificationContext;
import org.opendaylight.mdsal.dom.spi.shard.WriteableModificationNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;

/**
 * Factory for {@link DistributedShardModification}.
 */
@Deprecated(forRemoval = true)
public final class DistributedShardModificationFactory {
    private final Map<DOMDataTreeIdentifier, ForeignShardModificationContext> childShards;
    private final Map<PathArgument, WriteableModificationNode> children;
    private final DOMDataTreeIdentifier root;

    DistributedShardModificationFactory(final DOMDataTreeIdentifier root,
                                        final Map<PathArgument, WriteableModificationNode> children,
                                        final Map<DOMDataTreeIdentifier, ForeignShardModificationContext> childShards) {
        this.root = requireNonNull(root);
        this.children = ImmutableMap.copyOf(children);
        this.childShards = ImmutableMap.copyOf(childShards);
    }

    @VisibleForTesting
    Map<PathArgument, WriteableModificationNode> getChildren() {
        return children;
    }

    @VisibleForTesting
    Map<DOMDataTreeIdentifier, ForeignShardModificationContext> getChildShards() {
        return childShards;
    }

    DistributedShardModification createModification(final ClientTransaction transaction) {
        return new DistributedShardModification(
                new DistributedShardModificationContext(transaction, root), children, childShards);
    }
}
