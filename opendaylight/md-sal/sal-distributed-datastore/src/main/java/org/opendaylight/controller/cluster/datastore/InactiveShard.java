/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Ticker;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.raft.api.RaftActorAccess;
import org.opendaylight.controller.cluster.raft.state.InactiveState;
import org.opendaylight.controller.cluster.raft.state.StartingState;

@NonNullByDefault
final class InactiveShard extends ShardBehavior implements InactiveState {
    // FIXME: more invariants like config should be here

    InactiveShard(final ShardIdentifier id) {
        super(id, Ticker.systemTicker());
    }

    InactiveShard(final ShardIdentifier id, final Ticker ticker) {
        super(id, ticker);
    }

    @Override
    public StartingState toStarting(final RaftActorAccess actorAccess) {
        // FIXME: ShardStatsMXMean from Shard
        return new StartingShard(this, actorAccess, null);
    }
}
