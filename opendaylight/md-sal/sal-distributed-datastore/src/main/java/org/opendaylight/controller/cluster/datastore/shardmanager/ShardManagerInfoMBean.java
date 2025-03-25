/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.shardmanager;

import static java.util.Objects.requireNonNull;

import java.util.List;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.raft.api.RaftRole;

// TODO: can we do an MXBean instead?
public interface ShardManagerInfoMBean {
    /**
     * Behaviours we can switch to.
     */
    @NonNullByDefault
    enum TargetBehavior {
        Follower(RaftRole.Follower),
        Leader(RaftRole.Leader);

        private final RaftRole raftRole;

        TargetBehavior(final RaftRole raftRole) {
            this.raftRole = requireNonNull(raftRole);
        }

        public RaftRole raftRole() {
            return raftRole;
        }
    }

    /**
     * Returns the list of all the local shard names.
     *
     * @return a list of all the local shard names
     */
    List<String> getLocalShards();

    /**
     * Returns the overall sync status for all shards.
     *
     * @return true if all local shards are in sync with their corresponding leaders
     */
    boolean getSyncStatus();

    /**
     * Returns the name of the local member.
     *
     * @return the local member name
     */
    String getMemberName();

    /**
     * Switches the raft behavior of all the local shards to the newBehavior.
     *
     * @param targetBehavior a {@link TargetBehavior}
     * @param term when switching to the Leader specifies for which term the Shard would be the Leader. Any
     *             modifications made to state will be written with this term. This term will then be used by the Raft
     *             replication implementation to decide which modifications should stay and which ones should be
     *             removed. Ideally the term provided when switching to a new Leader should always be higher than the
     *             previous term.
     */
    void switchAllLocalShardsState(TargetBehavior targetBehavior, long term);

    /**
     * Switches the raft behavior of the shard specified by shardName to the newBehavior.
     *
     * @param shardName a shard that is local to this shard manager
     * @param targetBehavior a {@link TargetBehavior}
     * @param term when switching to the Leader specifies for which term the Shard would be the Leader. Any
     *             modifications made to state will be written with this term. This term will then be used by the Raft
     *             replication implementation to decide which modifications should stay and which ones should be
     *             removed. Ideally the term provided when switching to a new Leader should always be higher than the
     *             previous term.
     */
    void switchShardState(String shardName, TargetBehavior targetBehavior, long term);
}
