/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.policy;

/**
 * Testing policy suitable for scenarios, where the Leader can modify his state without waiting on consensus from his
 * Followers. Seen in 2-node deployments.
 */
public class IndependentLeaderTestRaftPolicy implements RaftPolicy {

    @Override
    public boolean automaticElectionsEnabled() {
        return true;
    }

    @Override
    public boolean applyModificationToStateBeforeConsensus() {
        return true;
    }
}