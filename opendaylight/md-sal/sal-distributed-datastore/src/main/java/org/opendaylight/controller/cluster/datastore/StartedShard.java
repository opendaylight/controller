/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.raft.state.InactiveState;
import org.opendaylight.controller.cluster.raft.state.RaftStateBehavior;
import org.opendaylight.controller.cluster.raft.state.StateSnapshot;

@NonNullByDefault
abstract sealed class StartedShard extends ActiveShard permits CandidateShard, ElectedShard, FollowerShard {
    final ShardDataTree dataTree;

    StartedShard(final StartingShard starting, final ShardDataTree dataTree) {
        super(starting);
        this.dataTree = requireNonNull(dataTree);
    }

    StartedShard(final StartedShard prev) {
        super(prev);
        dataTree = prev.dataTree;
    }

    /**
     * Returns the current {@link ABIVersion}.
     *
     * @return the payload version
     * @see RaftStateBehavior#payloadVersion()
     */
    @SuppressWarnings("static-method")
    public final short payloadVersion() {
        return ABIVersion.current().shortValue();
    }

    @Override
    InactiveState stop() {
        // FIXME: cleanup dataTree
        // FIXME: cleanup frontends?
        return super.stop();
    }

    public final StateSnapshot takeSnapshot() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }
}
