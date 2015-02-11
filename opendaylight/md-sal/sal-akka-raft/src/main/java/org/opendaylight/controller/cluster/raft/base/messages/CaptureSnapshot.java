/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.base.messages;

public class CaptureSnapshot {
    private long lastAppliedIndex;
    private long lastAppliedTerm;
    private long lastIndex;
    private long lastTerm;
    private boolean installSnapshotInitiated;
    private long replicatedToAllIndex;

    public CaptureSnapshot(long lastIndex, long lastTerm,
        long lastAppliedIndex, long lastAppliedTerm, long replicatedToAllIndex) {
        this(lastIndex, lastTerm, lastAppliedIndex, lastAppliedTerm, replicatedToAllIndex , false);
    }

    public CaptureSnapshot(long lastIndex, long lastTerm,long lastAppliedIndex,
        long lastAppliedTerm, long replicatedToAllIndex, boolean installSnapshotInitiated) {
        this.lastIndex = lastIndex;
        this.lastTerm = lastTerm;
        this.lastAppliedIndex = lastAppliedIndex;
        this.lastAppliedTerm = lastAppliedTerm;
        this.installSnapshotInitiated = installSnapshotInitiated;
        this.replicatedToAllIndex = replicatedToAllIndex;
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
}
