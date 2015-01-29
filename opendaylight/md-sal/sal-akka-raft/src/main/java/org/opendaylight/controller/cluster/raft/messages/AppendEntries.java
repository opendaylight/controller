/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.messages;

import com.google.protobuf.GeneratedMessage;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.ReplicatedLogImplEntry;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.controller.protobuff.messages.cluster.raft.AppendEntriesMessages;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Invoked by leader to replicate log entries (§5.3); also used as
 * heartbeat (§5.2).
 */
public class AppendEntries extends AbstractRaftRPC {
    public static final Class<AppendEntriesMessages.AppendEntries> SERIALIZABLE_CLASS = AppendEntriesMessages.AppendEntries.class;

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AppendEntries.class);
    private static final long serialVersionUID = 1L;

    // So that follower can redirect clients
    private final String leaderId;

    // Index of log entry immediately preceding new ones
    private final long prevLogIndex;

    // term of prevLogIndex entry
    private final long prevLogTerm;

    // log entries to store (empty for heartbeat;
    // may send more than one for efficiency)
    private final List<ReplicatedLogEntry> entries;

    // leader's commitIndex
    private final long leaderCommit;

    // index which has been replicated successfully to all followers, -1 if none
    private final long replicatedToAllIndex;

    public AppendEntries(long term, String leaderId, long prevLogIndex,
        long prevLogTerm, List<ReplicatedLogEntry> entries, long leaderCommit, long replicatedToAllIndex) {
        super(term);
        this.leaderId = leaderId;
        this.prevLogIndex = prevLogIndex;
        this.prevLogTerm = prevLogTerm;
        this.entries = entries;
        this.leaderCommit = leaderCommit;
        this.replicatedToAllIndex = replicatedToAllIndex;
    }

    public String getLeaderId() {
        return leaderId;
    }

    public long getPrevLogIndex() {
        return prevLogIndex;
    }

    public long getPrevLogTerm() {
        return prevLogTerm;
    }

    public List<ReplicatedLogEntry> getEntries() {
        return entries;
    }

    public long getLeaderCommit() {
        return leaderCommit;
    }

    public long getReplicatedToAllIndex() {
        return replicatedToAllIndex;
    }

    @Override public String toString() {
        final StringBuilder sb =
            new StringBuilder("AppendEntries{");
        sb.append("term=").append(getTerm());
        sb.append("leaderId='").append(leaderId).append('\'');
        sb.append(", prevLogIndex=").append(prevLogIndex);
        sb.append(", prevLogTerm=").append(prevLogTerm);
        sb.append(", entries=").append(entries);
        sb.append(", leaderCommit=").append(leaderCommit);
        sb.append(", replicatedToAllIndex=").append(replicatedToAllIndex);
        sb.append('}');
        return sb.toString();
    }

    public <T extends Object> Object toSerializable(){
        AppendEntriesMessages.AppendEntries.Builder to = AppendEntriesMessages.AppendEntries.newBuilder();
        to.setTerm(this.getTerm())
            .setLeaderId(this.getLeaderId())
            .setPrevLogTerm(this.getPrevLogTerm())
            .setPrevLogIndex(this.getPrevLogIndex())
            .setLeaderCommit(this.getLeaderCommit())
            .setReplicatedToAllIndex(this.getReplicatedToAllIndex());

        for (ReplicatedLogEntry logEntry : this.getEntries()) {

            AppendEntriesMessages.AppendEntries.ReplicatedLogEntry.Builder arBuilder =
                AppendEntriesMessages.AppendEntries.ReplicatedLogEntry.newBuilder();

            AppendEntriesMessages.AppendEntries.ReplicatedLogEntry.Payload.Builder arpBuilder =
                AppendEntriesMessages.AppendEntries.ReplicatedLogEntry.Payload.newBuilder();

            //get the client specific payload extensions and add them to the payload builder
            Map<GeneratedMessage.GeneratedExtension, T> map = logEntry.getData().encode();
            Iterator<Map.Entry<GeneratedMessage.GeneratedExtension, T>> iter = map.entrySet().iterator();

            while (iter.hasNext()) {
                Map.Entry<GeneratedMessage.GeneratedExtension, T> entry = iter.next();
                arpBuilder.setExtension(entry.getKey(), entry.getValue());
            }

            arpBuilder.setClientPayloadClassName(logEntry.getData().getClientPayloadClassName());

            arBuilder.setData(arpBuilder).setIndex(logEntry.getIndex()).setTerm(logEntry.getTerm());
            to.addLogEntries(arBuilder);
        }

        return to.build();
    }

    public static AppendEntries fromSerializable(Object o){
        AppendEntriesMessages.AppendEntries from = (AppendEntriesMessages.AppendEntries) o;

        List<ReplicatedLogEntry> logEntryList = new ArrayList<>();
        for (AppendEntriesMessages.AppendEntries.ReplicatedLogEntry leProtoBuff : from.getLogEntriesList()) {

            Payload payload = null ;
            try {
                if(leProtoBuff.getData() != null && leProtoBuff.getData().getClientPayloadClassName() != null) {
                    String clientPayloadClassName = leProtoBuff.getData().getClientPayloadClassName();
                    payload = (Payload) Class.forName(clientPayloadClassName).newInstance();
                    payload = payload.decode(leProtoBuff.getData());
                    payload.setClientPayloadClassName(clientPayloadClassName);
                } else {
                    LOG.error("Payload is null or payload does not have client payload class name");
                }

            } catch (InstantiationException e) {
                LOG.error("InstantiationException when instantiating "+leProtoBuff.getData().getClientPayloadClassName(), e);
            } catch (IllegalAccessException e) {
                LOG.error("IllegalAccessException when accessing "+leProtoBuff.getData().getClientPayloadClassName(), e);
            } catch (ClassNotFoundException e) {
                LOG.error("ClassNotFoundException when loading "+leProtoBuff.getData().getClientPayloadClassName(), e);
            }
            ReplicatedLogEntry logEntry = new ReplicatedLogImplEntry(
                leProtoBuff.getIndex(), leProtoBuff.getTerm(), payload);
            logEntryList.add(logEntry);
        }

        AppendEntries to = new AppendEntries(from.getTerm(),
            from.getLeaderId(),
            from.getPrevLogIndex(),
            from.getPrevLogTerm(),
            logEntryList,
            from.getLeaderCommit(),
            from.getReplicatedToAllIndex());

        return to;
    }
}
