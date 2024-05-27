/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.policy;

public class DefaultRaftPolicy implements RaftPolicy {

    public static final RaftPolicy INSTANCE = new DefaultRaftPolicy();

    @Override
    public boolean automaticElectionsEnabled() {
        return true;
    }

    @Override
    public boolean applyModificationToStateBeforeConsensus() {
        return false;
    }
}
