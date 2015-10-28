/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.policy;

/**
 * DisableElectionsRaftPolicy can be used for actors that does not
 * participate in shard election. Modification to state would
 * still require consensus.
 */

public class DisableElectionsRaftPolicy implements RaftPolicy {
    @Override
    public boolean automaticElectionsEnabled() {
        return false;
    }

    @Override
    public boolean applyModificationToStateBeforeConsensus() {
        return false;
    }
}
