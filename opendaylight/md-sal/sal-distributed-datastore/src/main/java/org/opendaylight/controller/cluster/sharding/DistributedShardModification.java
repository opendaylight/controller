/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.sharding;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteCursor;
import org.opendaylight.mdsal.dom.spi.shard.ForeignShardModificationContext;
import org.opendaylight.mdsal.dom.spi.shard.WritableNodeOperation;
import org.opendaylight.mdsal.dom.spi.shard.WriteCursorStrategy;
import org.opendaylight.mdsal.dom.spi.shard.WriteableModificationNode;
import org.opendaylight.mdsal.dom.spi.shard.WriteableNodeWithSubshard;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;

/**
 * Shard modification that consists of the whole shard context, provides cursors which correctly delegate to subshards
 * if any are present.
 */
@Deprecated(forRemoval = true)
public class DistributedShardModification extends WriteableNodeWithSubshard {

    private final DistributedShardModificationContext context;
    private final Map<DOMDataTreeIdentifier, ForeignShardModificationContext> childShards;

    public DistributedShardModification(final DistributedShardModificationContext context,
                                        final Map<PathArgument, WriteableModificationNode> subshards,
                                        final Map<DOMDataTreeIdentifier, ForeignShardModificationContext> childShards) {
        super(subshards);
        this.context = requireNonNull(context);
        this.childShards = requireNonNull(childShards);
    }

    @Override
    public PathArgument getIdentifier() {
        return context.getIdentifier().getRootIdentifier().getLastPathArgument();
    }

    @Override
    public WriteCursorStrategy createOperation(final DOMDataTreeWriteCursor parentCursor) {
        return new WritableNodeOperation(this, context.cursor()) {
            @Override
            public void exit() {
                throw new IllegalStateException("Can not exit data tree root");
            }
        };
    }

    void cursorClosed() {
        context.closeCursor();
    }

    DOMStoreThreePhaseCommitCohort seal() {
        childShards.values().stream().filter(ForeignShardModificationContext::isModified)
                .forEach(ForeignShardModificationContext::ready);

        return context.ready();
    }

    DOMDataTreeIdentifier getPrefix() {
        return context.getIdentifier();
    }

    Map<DOMDataTreeIdentifier, ForeignShardModificationContext> getChildShards() {
        return childShards;
    }
}
