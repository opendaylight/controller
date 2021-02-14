/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.sharding;

import org.opendaylight.mdsal.dom.spi.shard.AbstractDataModificationCursor;
import org.opendaylight.mdsal.dom.spi.shard.WriteCursorStrategy;

/**
 * Internal cursor implementation consisting of WriteCursorStrategies which forwards writes to foreign modifications
 * if any.
 */
@Deprecated(forRemoval = true)
public class DistributedShardModificationCursor extends AbstractDataModificationCursor<DistributedShardModification> {

    private final ShardProxyTransaction parent;

    public DistributedShardModificationCursor(final DistributedShardModification root,
                                              final ShardProxyTransaction parent) {
        super(root);
        this.parent = parent;
    }

    @Override
    protected WriteCursorStrategy getRootOperation(final DistributedShardModification root) {
        return root.createOperation(null);
    }

    @Override
    public void close() {
        parent.cursorClosed();
    }
}
