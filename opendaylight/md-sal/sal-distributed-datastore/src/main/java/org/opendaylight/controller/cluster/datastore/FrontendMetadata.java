/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.persisted.FrontendShardDataTreeSnapshotMetadata;

/**
 * Frontend state as observed by a shard follower. This class is responsible for maintaining metadata state
 * so that this can be used to seed {@link LeaderFrontendState} with proper state so that the frontend/backend
 * conversation can continue where it left off.
 *
 * @author Robert Varga
 */
@NotThreadSafe
final class FrontendMetadata extends ShardDataTreeMetadata<FrontendShardDataTreeSnapshotMetadata> {
    private final Map<FrontendIdentifier, FollowerFrontendState> knownFrontends = new HashMap<>();

    @Override
    void reset() {
        knownFrontends.clear();
    }

    @Override
    void doApplySnapshot(final FrontendShardDataTreeSnapshotMetadata snapshot) {
        // TODO Auto-generated method stub

    }

    @Override
    Class<FrontendShardDataTreeSnapshotMetadata> getSupportedType() {
        return FrontendShardDataTreeSnapshotMetadata.class;
    }

    @Override
    FrontendShardDataTreeSnapshotMetadata toStapshot() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    void onTransactionCommitted(final TransactionIdentifier txId) {
        // TODO Auto-generated method stub

    }

}
