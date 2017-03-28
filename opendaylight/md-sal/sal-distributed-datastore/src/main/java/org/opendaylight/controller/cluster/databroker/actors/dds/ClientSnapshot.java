/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Snapshot of the datastore state. Note this snapshot is not consistent across shards because sub-shard snapshots are
 * created lazily.
 *
 * @author Robert Varga
 */
@Beta
public class ClientSnapshot extends AbstractClientHandle<AbstractProxyTransaction> {
    // Hidden to prevent outside instantiation
    ClientSnapshot(final AbstractClientHistory parent, final TransactionIdentifier transactionId) {
        super(parent, transactionId);
    }

    private AbstractProxyTransaction createProxy(final Long shard) {
        return parent().createSnapshotProxy(getIdentifier(), shard);
    }

    private AbstractProxyTransaction ensureSnapshotProxy(final YangInstanceIdentifier path) {
        return ensureProxy(path, this::createProxy);
    }

    public CheckedFuture<Boolean, ReadFailedException> exists(final YangInstanceIdentifier path) {
        return ensureSnapshotProxy(path).exists(path);
    }

    public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(
            final YangInstanceIdentifier path) {
        return ensureSnapshotProxy(path).read(path);
    }
}
