/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.policy;


import org.opendaylight.controller.cluster.raft.policy.RaftPolicy;

/**
 * A RaftPolicy that disables elections so that we can then specify exactly which Shard Replica should be
 * Leader. Once a Leader is assigned it will behave as per Raft.
 */
public class TestOnlyRaftPolicy implements RaftPolicy {
    @Override
    public boolean automaticElectionsEnabled() {
        return false;
    }

    @Override
    public boolean applyModificationToStateBeforeConsensus() {
        return false;
    }
}
