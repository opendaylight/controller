/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.messages;

import com.google.protobuf.GeneratedMessage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.opendaylight.controller.cluster.raft.RaftVersions;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.ReplicatedLogImplEntry;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.controller.protobuff.messages.cluster.raft.AppendEntriesMessages;

/**
 * Invoked by leader to replicate log entries (§5.3); also used as
 * heartbeat (§5.2).
 */
public class AppendEntries extends AbstractRaftRPC {
    private static final long serialVersionUID = 1L;

    public static final Class<AppendEntriesMessages.AppendEntries> LEGACY_SERIALIZABLE_CLASS =
            AppendEntriesMessages.AppendEntries.class;

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AppendEntries.class);

    // So that follower can redirect clients
    private final String leaderId;

    // Index of log entry immediately preceding new ones
    private final long prevLogIndex;

    // term of prevLogIndex entry
    private final long prevLogTerm;

    // log entries to store (empty for heartbeat;
    // may send more than one for efficiency)
    private transient List<ReplicatedLogEntry> entries;

    // leader's commitIndex
    private final long leaderCommit;

    public AppendEntries(long term, String leaderId, long prevLogIndex,
        long prevLogTerm, List<ReplicatedLogEntry> entries, long leaderCommit) {
        super(term);
        this.leaderId = leaderId;
        this.prevLogIndex = prevLogIndex;
        this.prevLogTerm = prevLogTerm;
        this.entries = entries;
        this.leaderCommit = leaderCommit;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeShort(RaftVersions.CURRENT_VERSION);
        out.defaultWriteObject();

        out.writeInt(entries.size());
        for(ReplicatedLogEntry e: entries) {
            out.writeObject(e);
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.readShort(); // version

        in.defaultReadObject();

        int size = in.readInt();
        entries = new ArrayList<>(size);
        for(int i = 0; i < size; i++) {
            entries.add((ReplicatedLogEntry) in.readObject());
        }
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

    @Override
    public String toString() {
        final StringBuilder sb =
            new StringBuilder("AppendEntries{");
        sb.append("term=").append(getTerm());
        sb.append("leaderId='").append(leaderId).append('\'');
        sb.append(", prevLogIndex=").append(prevLogIndex);
        sb.append(", prevLogTerm=").append(prevLogTerm);
        sb.append(", entries=").append(entries);
        sb.append(", leaderCommit=").append(leaderCommit);
        sb.append('}');
        return sb.toString();
    }

    public <T extends Object> Object toSerializable() {
        return toSerializable(RaftVersions.CURRENT_VERSION);
    }

    public <T extends Object> Object toSerializable(short version) {
        if(version < RaftVersions.LITHIUM_VERSION) {
            return toLegacySerializable();
        } else {
            return this;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <T> Object toLegacySerializable() {
        AppendEntriesMessages.AppendEntries.Builder to = AppendEntriesMessages.AppendEntries.newBuilder();
        to.setTerm(this.getTerm())
            .setLeaderId(this.getLeaderId())
            .setPrevLogTerm(this.getPrevLogTerm())
            .setPrevLogIndex(this.getPrevLogIndex())
            .setLeaderCommit(this.getLeaderCommit());

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

    public static AppendEntries fromSerializable(Object serialized) {
        if(serialized instanceof AppendEntries) {
            return (AppendEntries)serialized;
        }
        else {
            return fromLegacySerializable((AppendEntriesMessages.AppendEntries) serialized);
        }
    }

    private static AppendEntries fromLegacySerializable(AppendEntriesMessages.AppendEntries from) {
        List<ReplicatedLogEntry> logEntryList = new ArrayList<>();
        for (AppendEntriesMessages.AppendEntries.ReplicatedLogEntry leProtoBuff : from.getLogEntriesList()) {

            Payload payload = null ;
            try {
                if(leProtoBuff.getData() != null && leProtoBuff.getData().getClientPayloadClassName() != null) {
                    String clientPayloadClassName = leProtoBuff.getData().getClientPayloadClassName();
                    payload = (Payload) Class.forName(clientPayloadClassName).newInstance();
                    payload = payload.decode(leProtoBuff.getData());
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
            from.getLeaderCommit());

        return to;
    }

    public static boolean isSerializedType(Object message) {
        return message instanceof AppendEntries || LEGACY_SERIALIZABLE_CLASS.isInstance(message);
    }
}
