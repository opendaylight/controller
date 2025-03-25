/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.policy;

/**
 * The RaftPolicy is intended to change the default behavior of Raft. For example we may want to be able to determine
 * which Raft replica should become the leader - with Raft elections are randomized so it is not possible to specify
 * which replica should be the leader. The ability to specify the leader would be quite useful when testing a raft
 * cluster.
 *
 * <p>Similarly we may want to customize when exactly we apply a modification to the state - with Raft a modification
 * is only applied to the state when the modification is replicated to a majority of the replicas. The ability to apply
 * a modification to the state before consensus would be useful in scenarios where you have only 2 nodes in a Raft
 * cluster and one of them is down but you still want the RaftActor to apply a modification to the state.
 */
public interface RaftPolicy {
    /**
     * According to Raft a Follower which does not receive a heartbeat (aka AppendEntries) in a given period should
     * become a Candidate and trigger an election.
     *
     * @return true to enable automatic Raft elections, false to disable them
     */
    boolean automaticElectionsEnabled();

    /**
     * According to Raft consensus on a Raft entry is achieved only after a Leader replicates a log entry to a
     * majority of it's followers.
     *
     * @return true if modification should be applied before consensus, false to apply modification to state
     *     as per Raft
     */
    boolean applyModificationToStateBeforeConsensus();
}
