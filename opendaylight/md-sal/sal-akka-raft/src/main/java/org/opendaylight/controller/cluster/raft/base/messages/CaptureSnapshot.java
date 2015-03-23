/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.base.messages;

public class CaptureSnapshot {
    private final long lastAppliedIndex;
    private final long lastAppliedTerm;
    private final long lastIndex;
    private final long lastTerm;
    private final boolean installSnapshotInitiated;
    private final long replicatedToAllIndex;
    private final long replicatedToAllTerm;

    public CaptureSnapshot(long lastIndex, long lastTerm,
        long lastAppliedIndex, long lastAppliedTerm, long replicatedToAllIndex, long replicatedToAllTerm) {
        this(lastIndex, lastTerm, lastAppliedIndex, lastAppliedTerm, replicatedToAllIndex , replicatedToAllTerm, false);
    }

    public CaptureSnapshot(long lastIndex, long lastTerm,long lastAppliedIndex,
        long lastAppliedTerm, long replicatedToAllIndex, long replicatedToAllTerm, boolean installSnapshotInitiated) {
        this.lastIndex = lastIndex;
        this.lastTerm = lastTerm;
        this.lastAppliedIndex = lastAppliedIndex;
        this.lastAppliedTerm = lastAppliedTerm;
        this.installSnapshotInitiated = installSnapshotInitiated;
        this.replicatedToAllIndex = replicatedToAllIndex;
        this.replicatedToAllTerm = replicatedToAllTerm;
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

    public boolean isInstallSnapshotInitiated() {
        return installSnapshotInitiated;
    }

    public long getReplicatedToAllIndex() {
        return replicatedToAllIndex;
    }

    public long getReplicatedToAllTerm() {
        return replicatedToAllTerm;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CaptureSnapshot [lastAppliedIndex=").append(lastAppliedIndex).append(", lastAppliedTerm=")
                .append(lastAppliedTerm).append(", lastIndex=").append(lastIndex).append(", lastTerm=")
                .append(lastTerm).append(", installSnapshotInitiated=").append(installSnapshotInitiated)
                .append(", replicatedToAllIndex=").append(replicatedToAllIndex).append(", replicatedToAllTerm=")
                .append(replicatedToAllTerm).append("]");
        return builder.toString();
    }
}
