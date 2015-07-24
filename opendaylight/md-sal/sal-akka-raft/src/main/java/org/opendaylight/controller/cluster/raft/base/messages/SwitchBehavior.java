/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.base.messages;

import org.opendaylight.controller.cluster.raft.RaftState;

public class SwitchBehavior {
    private final RaftState newState;
    private final long newTerm;

    public SwitchBehavior(RaftState newState, long newTerm) {
        this.newState = newState;
        this.newTerm = newTerm;
    }

    public RaftState getNewState() {
        return newState;
    }

    public long getNewTerm() {
        return newTerm;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SwitchBehavior{");
        sb.append("newState=").append(newState);
        sb.append(", newTerm=").append(newTerm);
        sb.append('}');
        return sb.toString();
    }
}
