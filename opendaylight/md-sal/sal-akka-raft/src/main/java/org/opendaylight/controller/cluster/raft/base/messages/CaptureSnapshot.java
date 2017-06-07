/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.base.messages;

import akka.dispatch.ControlMessage;
import java.util.Collections;
import java.util.List;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;

public class CaptureSnapshot implements ControlMessage {
    private final long lastAppliedIndex;
    private final long lastAppliedTerm;
    private final long lastIndex;
    private final long lastTerm;
    private final long replicatedToAllIndex;
    private final long replicatedToAllTerm;
    private final List<ReplicatedLogEntry> unAppliedEntries;

    public CaptureSnapshot(long lastIndex, long lastTerm, long lastAppliedIndex,
            long lastAppliedTerm, long replicatedToAllIndex, long replicatedToAllTerm,
            List<ReplicatedLogEntry> unAppliedEntries) {
        this.lastIndex = lastIndex;
        this.lastTerm = lastTerm;
        this.lastAppliedIndex = lastAppliedIndex;
        this.lastAppliedTerm = lastAppliedTerm;
        this.replicatedToAllIndex = replicatedToAllIndex;
        this.replicatedToAllTerm = replicatedToAllTerm;
        this.unAppliedEntries = unAppliedEntries != null ? unAppliedEntries :
            Collections.<ReplicatedLogEntry>emptyList();
    }

    public long getLastAppliedIndex() {
        return lastAppliedIndex;
    }

    public long getLastAppliedTerm() {
        return lastAppliedTerm;
    }

    public long getLastIndex() {
        return lastIndex;
    }

    public long getLastTerm() {
        return lastTerm;
    }

    public long getReplicatedToAllIndex() {
        return replicatedToAllIndex;
    }

    public long getReplicatedToAllTerm() {
        return replicatedToAllTerm;
    }

    public List<ReplicatedLogEntry> getUnAppliedEntries() {
        return unAppliedEntries;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CaptureSnapshot [lastAppliedIndex=").append(lastAppliedIndex).append(", lastAppliedTerm=")
                .append(lastAppliedTerm).append(", lastIndex=").append(lastIndex).append(", lastTerm=")
                .append(lastTerm).append(", installSnapshotInitiated=")
                .append(", replicatedToAllIndex=").append(replicatedToAllIndex).append(", replicatedToAllTerm=")
                .append(replicatedToAllTerm).append(", unAppliedEntries size=")
                .append(unAppliedEntries.size()).append("]");
        return builder.toString();
    }


}
