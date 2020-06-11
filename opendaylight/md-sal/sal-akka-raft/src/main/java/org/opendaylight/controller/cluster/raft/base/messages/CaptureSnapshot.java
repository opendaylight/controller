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
    private final boolean mandatoryTrim;

    public CaptureSnapshot(long lastIndex, long lastTerm, long lastAppliedIndex,
            long lastAppliedTerm, long replicatedToAllIndex, long replicatedToAllTerm,
            List<ReplicatedLogEntry> unAppliedEntries, boolean mandatoryTrim) {
        this.lastIndex = lastIndex;
        this.lastTerm = lastTerm;
        this.lastAppliedIndex = lastAppliedIndex;
        this.lastAppliedTerm = lastAppliedTerm;
        this.replicatedToAllIndex = replicatedToAllIndex;
        this.replicatedToAllTerm = replicatedToAllTerm;
        this.unAppliedEntries = unAppliedEntries != null ? unAppliedEntries :
            Collections.<ReplicatedLogEntry>emptyList();
        this.mandatoryTrim = mandatoryTrim;
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

    public boolean isMandatoryTrim() {
        return mandatoryTrim;
    }

    @Override
    public String toString() {
        return "CaptureSnapshot [lastAppliedIndex=" + lastAppliedIndex
                + ", lastAppliedTerm=" + lastAppliedTerm
                + ", lastIndex=" + lastIndex
                + ", lastTerm=" + lastTerm
                + ", installSnapshotInitiated="
                + ", replicatedToAllIndex=" + replicatedToAllIndex
                + ", replicatedToAllTerm=" + replicatedToAllTerm
                + ", unAppliedEntries size=" + unAppliedEntries.size()
                + ", mandatoryTrim=" + mandatoryTrim + "]";
    }


}
