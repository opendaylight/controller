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
 * The TwoNodeClusterRaftPolicy is intended to be used in a two node deployment where when one instance
 * of the controller goes down the other instance is to take over and move the state forward.
 * When a TwoNodeClusterRaftPolicy is used Raft elections are disabled. This is primarily because we would
 * need to specify the leader externally. Also since we want one node to continue to function while the other
 * node is down we would need to apply a modification to the state before consensus occurs.
 */
public class TwoNodeClusterRaftPolicy implements RaftPolicy {
    @Override
    public boolean automaticElectionsEnabled() {
        return false;
    }

    @Override
    public boolean applyModificationToStateBeforeConsensus() {
        return true;
    }
}
