/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.google.common.base.Preconditions;
import com.google.protobuf.GeneratedMessage;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.controller.protobuff.messages.cluster.raft.AppendEntriesMessages;
import org.opendaylight.controller.protobuff.messages.cluster.raft.test.MockPayloadMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockRaftActorContext implements RaftActorContext {

    private String id;
    private ActorSystem system;
    private ActorRef actor;
    private long index = 0;
    private long lastApplied = 0;
    private final ElectionTerm electionTerm;
    private ReplicatedLog replicatedLog;
    private Map<String, String> peerAddresses = new HashMap<>();
    private ConfigParams configParams;
    private boolean snapshotCaptureInitiated;

    public MockRaftActorContext(){
        electionTerm = new ElectionTerm() {
            private long currentTerm = 1;
            private String votedFor = "";

            @Override
            public long getCurrentTerm() {
                return currentTerm;
            }

            @Override
            public String getVotedFor() {
                return votedFor;
            }

            @Override
            public void update(long currentTerm, String votedFor){
                this.currentTerm = currentTerm;
                this.votedFor = votedFor;

                // TODO : Write to some persistent state
            }

            @Override public void updateAndPersist(long currentTerm,
                String votedFor) {
                update(currentTerm, votedFor);
            }
        };

        configParams = new DefaultConfigParamsImpl();
    }

    public MockRaftActorContext(String id, ActorSystem system, ActorRef actor){
        this();
        this.id = id;
        this.system = system;
        this.actor = actor;

        initReplicatedLog();
    }


    public void initReplicatedLog(){
        this.replicatedLog = new SimpleReplicatedLog();
        long term = getTermInformation().getCurrentTerm();
        this.replicatedLog.append(new MockReplicatedLogEntry(term, 0, new MockPayload("1")));
        this.replicatedLog.append(new MockReplicatedLogEntry(term, 1, new MockPayload("2")));
    }

    @Override public ActorRef actorOf(Props props) {
        return system.actorOf(props);
    }

    @Override public ActorSelection actorSelection(String path) {
        return system.actorSelection(path);
    }

    @Override public String getId() {
        return id;
    }

    @Override public ActorRef getActor() {
        return actor;
    }

    @Override public ElectionTerm getTermInformation() {
        return electionTerm;
    }

    public void setIndex(long index){
        this.index = index;
    }

    @Override public long getCommitIndex() {
        return index;
    }

    @Override public void setCommitIndex(long commitIndex) {
        this.index = commitIndex;
    }

    @Override public void setLastApplied(long lastApplied){
        this.lastApplied = lastApplied;
    }

    @Override public long getLastApplied() {
        return lastApplied;
    }

    @Override
    // FIXME : A lot of tests try to manipulate the replicated log by setting it using this method
    // This is OK to do if the underlyingActor is not RafActor or a derived class. If not then you should not
    // used this way to manipulate the log because the RaftActor actually has a field replicatedLog
    // which it creates internally and sets on the RaftActorContext
    // The only right way to manipulate the replicated log therefore is to get it from either the RaftActor
    // or the RaftActorContext and modify the entries in there instead of trying to replace it by using this setter
    // Simple assertion that will fail if you do so
    // ReplicatedLog log = new ReplicatedLogImpl();
    // raftActor.underlyingActor().getRaftActorContext().setReplicatedLog(log);
    // assertEquals(log, raftActor.underlyingActor().getReplicatedLog())
    public void setReplicatedLog(ReplicatedLog replicatedLog) {
        this.replicatedLog = replicatedLog;
    }

    @Override public ReplicatedLog getReplicatedLog() {
        return replicatedLog;
    }

    @Override public ActorSystem getActorSystem() {
        return this.system;
    }

    @Override public Logger getLogger() {
        return LoggerFactory.getLogger(getClass());
    }

    @Override public Map<String, String> getPeerAddresses() {
        return peerAddresses;
    }

    @Override public String getPeerAddress(String peerId) {
        return peerAddresses.get(peerId);
    }

    @Override public void addToPeers(String name, String address) {
        peerAddresses.put(name, address);
    }

    @Override public void removePeer(String name) {
        peerAddresses.remove(name);
    }

    @Override public ActorSelection getPeerActorSelection(String peerId) {
        String peerAddress = getPeerAddress(peerId);
        if(peerAddress != null){
            return actorSelection(peerAddress);
        }
        return null;
    }

    @Override public void setPeerAddress(String peerId, String peerAddress) {
        Preconditions.checkState(peerAddresses.containsKey(peerId));
        peerAddresses.put(peerId, peerAddress);
    }

    public void setPeerAddresses(Map<String, String> peerAddresses) {
        this.peerAddresses = peerAddresses;
    }

    @Override
    public ConfigParams getConfigParams() {
        return configParams;
    }

    @Override
    public void setSnapshotCaptureInitiated(boolean snapshotCaptureInitiated) {
        this.snapshotCaptureInitiated = snapshotCaptureInitiated;
    }

    @Override
    public boolean isSnapshotCaptureInitiated() {
        return snapshotCaptureInitiated;
    }

    public void setConfigParams(ConfigParams configParams) {
        this.configParams = configParams;
    }

    public static class SimpleReplicatedLog extends AbstractReplicatedLogImpl {
        @Override public void appendAndPersist(
            ReplicatedLogEntry replicatedLogEntry) {
            append(replicatedLogEntry);
        }

        @Override
        public int dataSize() {
            return -1;
        }

        @Override public void removeFromAndPersist(long index) {
            removeFrom(index);
        }
    }

    public static class MockPayload extends Payload implements Serializable {
        private static final long serialVersionUID = 3121380393130864247L;
        private String value = "";

        public MockPayload(){

        }

        public MockPayload(String s) {
            this.value = s;
        }

        @Override public  Map<GeneratedMessage.GeneratedExtension, String> encode() {
            Map<GeneratedMessage.GeneratedExtension, String> map = new HashMap<GeneratedMessage.GeneratedExtension, String>();
            map.put(MockPayloadMessages.value, value);
            return map;
        }

        @Override public Payload decode(
            AppendEntriesMessages.AppendEntries.ReplicatedLogEntry.Payload payloadProtoBuff) {
            String value = payloadProtoBuff.getExtension(MockPayloadMessages.value);
            this.value = value;
            return this;
        }

        @Override
        public int size() {
            return value.length();
        }

        @Override public String getClientPayloadClassName() {
            return MockPayload.class.getName();
        }

        @Override
        public String toString() {
            return value;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((value == null) ? 0 : value.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            MockPayload other = (MockPayload) obj;
            if (value == null) {
                if (other.value != null) {
                    return false;
                }
            } else if (!value.equals(other.value)) {
                return false;
            }
            return true;
        }
    }

    public static class MockReplicatedLogEntry implements ReplicatedLogEntry, Serializable {
        private static final long serialVersionUID = 1L;

        private final long term;
        private final long index;
        private final Payload data;

        public MockReplicatedLogEntry(long term, long index, Payload data){

            this.term = term;
            this.index = index;
            this.data = data;
        }

        @Override public Payload getData() {
            return data;
        }

        @Override public long getTerm() {
            return term;
        }

        @Override public long getIndex() {
            return index;
        }

        @Override
        public int size() {
            return getData().size();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((data == null) ? 0 : data.hashCode());
            result = prime * result + (int) (index ^ (index >>> 32));
            result = prime * result + (int) (term ^ (term >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            MockReplicatedLogEntry other = (MockReplicatedLogEntry) obj;
            if (data == null) {
                if (other.data != null) {
                    return false;
                }
            } else if (!data.equals(other.data)) {
                return false;
            }
            if (index != other.index) {
                return false;
            }
            if (term != other.term) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("MockReplicatedLogEntry [term=").append(term).append(", index=").append(index)
                    .append(", data=").append(data).append("]");
            return builder.toString();
        }
    }

    public static class MockReplicatedLogBuilder {
        private final ReplicatedLog mockLog = new SimpleReplicatedLog();

        public  MockReplicatedLogBuilder createEntries(int start, int end, int term) {
            for (int i=start; i<end; i++) {
                this.mockLog.append(new ReplicatedLogImplEntry(i, term, new MockRaftActorContext.MockPayload("foo" + i)));
            }
            return this;
        }

        public  MockReplicatedLogBuilder addEntry(int index, int term, MockPayload payload) {
            this.mockLog.append(new ReplicatedLogImplEntry(index, term, payload));
            return this;
        }

        public ReplicatedLog build() {
            return this.mockLog;
        }
    }
}
