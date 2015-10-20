/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.policy;

import org.opendaylight.controller.cluster.raft.policy.RaftPolicy;

/**
 * This RaftPolicy will be used for shards that are newly created after
 * the cluster is deployed. These actors does not participate in leader
 * shard election.
 */

public class ShardInitRaftPolicy implements RaftPolicy {
    @Override
    public boolean automaticElectionsEnabled() {
        return false;
    }

    @Override
    public boolean applyModificationToStateBeforeConsensus() {
        return false;
    }
}
