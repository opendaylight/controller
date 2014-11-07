/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.messages;

import com.google.protobuf.ByteString;
import org.opendaylight.controller.protobuff.messages.cluster.raft.InstallSnapshotMessages;

public class InstallSnapshot extends AbstractRaftRPC {

    public static final Class<InstallSnapshotMessages.InstallSnapshot> SERIALIZABLE_CLASS = InstallSnapshotMessages.InstallSnapshot.class;

    private final String leaderId;
    private final long lastIncludedIndex;
    private final long lastIncludedTerm;
    private final ByteString data;
    private final int chunkIndex;
    private final int totalChunks;

    public InstallSnapshot(long term, String leaderId, long lastIncludedIndex,
        long lastIncludedTerm, ByteString data, int chunkIndex, int totalChunks) {
        super(term);
        this.leaderId = leaderId;
        this.lastIncludedIndex = lastIncludedIndex;
        this.lastIncludedTerm = lastIncludedTerm;
        this.data = data;
        this.chunkIndex = chunkIndex;
        this.totalChunks = totalChunks;
    }

    public String getLeaderId() {
        return leaderId;
    }

    public long getLastIncludedIndex() {
        return lastIncludedIndex;
    }

    public long getLastIncludedTerm() {
        return lastIncludedTerm;
    }

    public ByteString getData() {
        return data;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public <T extends Object> Object toSerializable(){
        return InstallSnapshotMessages.InstallSnapshot.newBuilder()
            .setLeaderId(this.getLeaderId())
            .setChunkIndex(this.getChunkIndex())
            .setData(this.getData())
            .setLastIncludedIndex(this.getLastIncludedIndex())
            .setLastIncludedTerm(this.getLastIncludedTerm())
            .setTotalChunks(this.getTotalChunks()).build();

    }

    public static InstallSnapshot fromSerializable (Object o) {
        InstallSnapshotMessages.InstallSnapshot from =
            (InstallSnapshotMessages.InstallSnapshot) o;

        InstallSnapshot installSnapshot = new InstallSnapshot(from.getTerm(),
            from.getLeaderId(), from.getLastIncludedIndex(),
            from.getLastIncludedTerm(), from.getData(),
            from.getChunkIndex(), from.getTotalChunks());

        return installSnapshot;
    }
}
