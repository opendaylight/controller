/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.raft.RaftState;

public final class SwitchShardBehavior {
    private final String shardName;
    private final RaftState newState;
    private final long term;

    public SwitchShardBehavior(String shardName, RaftState newState, long term) {
        this.shardName = Preconditions.checkNotNull(shardName);
        this.newState = Preconditions.checkNotNull(newState);
        this.term = term;
    }

    public String getShardName() {
        return shardName;
    }

    public RaftState getNewState() {
        return newState;
    }

    public long getTerm() {
        return term;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SwitchShardBehavior{");
        sb.append("shardName='").append(shardName).append('\'');
        sb.append(", newState='").append(newState).append('\'');
        sb.append(", term=").append(term);
        sb.append('}');
        return sb.toString();
    }
}
