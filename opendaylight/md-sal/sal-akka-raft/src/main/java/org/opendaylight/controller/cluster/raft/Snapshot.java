/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import java.io.Serializable;
import java.util.List;


public class Snapshot implements Serializable {
    private static final long serialVersionUID = -8298574936724056236L;

    private final byte[] state;
    private final List<ReplicatedLogEntry> unAppliedEntries;
    private final long lastIndex;
    private final long lastTerm;
    private final long lastAppliedIndex;
    private final long lastAppliedTerm;

    private Snapshot(byte[] state,
        List<ReplicatedLogEntry> unAppliedEntries, long lastIndex,
        long lastTerm, long lastAppliedIndex, long lastAppliedTerm) {
        this.state = state;
        this.unAppliedEntries = unAppliedEntries;
        this.lastIndex = lastIndex;
        this.lastTerm = lastTerm;
        this.lastAppliedIndex = lastAppliedIndex;
        this.lastAppliedTerm = lastAppliedTerm;
    }


    public static Snapshot create(byte[] state,
        List<ReplicatedLogEntry> entries, long lastIndex, long lastTerm,
        long lastAppliedIndex, long lastAppliedTerm) {
        return new Snapshot(state, entries, lastIndex, lastTerm,
            lastAppliedIndex, lastAppliedTerm);
    }

    public byte[] getState() {
        return state;
    }

    public List<ReplicatedLogEntry> getUnAppliedEntries() {
        return unAppliedEntries;
    }

    public long getLastTerm() {
        return lastTerm;
    }

    public long getLastAppliedIndex() {
        return lastAppliedIndex;
    }

    public long getLastAppliedTerm() {
        return lastAppliedTerm;
    }

    public long getLastIndex() {
        return this.lastIndex;
    }

    public String getLogMessage() {
        StringBuilder sb = new StringBuilder();
        return sb.append("Snapshot={")
            .append("lastTerm:" + this.getLastTerm() + ", ")
            .append("lastIndex:" + this.getLastIndex()  + ", ")
            .append("LastAppliedIndex:" + this.getLastAppliedIndex()  + ", ")
            .append("LastAppliedTerm:" + this.getLastAppliedTerm()  + ", ")
            .append("UnAppliedEntries size:" + this.getUnAppliedEntries().size()  + "}")
            .toString();

    }
}
