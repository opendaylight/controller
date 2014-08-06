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
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessage;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.controller.cluster.raft.protobuff.messages.AppendEntriesMessages;
import org.opendaylight.controller.cluster.raft.protobuff.messages.MockPayloadMessages;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockRaftActorContext implements RaftActorContext {

    private String id;
    private ActorSystem system;
    private ActorRef actor;
    private long index = 0;
    private long lastApplied = 0;
    private final ElectionTerm electionTerm;
    private ReplicatedLog replicatedLog;
    private Map<String, String> peerAddresses = new HashMap();

    public MockRaftActorContext(){
        electionTerm = null;

        initReplicatedLog();
    }

    public MockRaftActorContext(String id, ActorSystem system, ActorRef actor){
        this.id = id;
        this.system = system;
        this.actor = actor;

        final String id1 = id;
        electionTerm = new ElectionTerm() {
            /**
             * Identifier of the actor whose election term information this is
             */
            private final String id = id1;
            private long currentTerm = 0;
            private String votedFor = "";

            public long getCurrentTerm() {
                return currentTerm;
            }

            public String getVotedFor() {
                return votedFor;
            }

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

        initReplicatedLog();
    }


    public void initReplicatedLog(){
        this.replicatedLog = new SimpleReplicatedLog();
        this.replicatedLog.append(new MockReplicatedLogEntry(1, 1, new MockPayload("")));
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

    public void setReplicatedLog(ReplicatedLog replicatedLog) {
        this.replicatedLog = replicatedLog;
    }

    @Override public ReplicatedLog getReplicatedLog() {
        return replicatedLog;
    }

    @Override public ActorSystem getActorSystem() {
        return this.system;
    }

    @Override public LoggingAdapter getLogger() {
        return Logging.getLogger(system, this);
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
        return new DefaultConfigParamsImpl();
    }

    //TODO: we should extend AbstractReplicatedLogImpl and let it handle all the logic
    public static class SimpleReplicatedLog implements ReplicatedLog {
        private final List<ReplicatedLogEntry> log = new ArrayList<>();
        private long snapshotIndex = -1;

        protected int adjustedIndex(long logEntryIndex) {
            if(snapshotIndex < 0){
                return (int) logEntryIndex;
            }
            return (int) (logEntryIndex - (snapshotIndex + 1));
        }

        @Override public ReplicatedLogEntry get(long logEntryIndex) {
            long adjustedIndex = adjustedIndex(logEntryIndex);

            if(adjustedIndex >= log.size() || adjustedIndex < 0){
                return null;
            }
            return log.get((int) adjustedIndex);
        }

        @Override public ReplicatedLogEntry last() {
            if(log.size() == 0){
                return null;
            }
            return log.get(log.size()-1);
        }

        @Override public long lastIndex() {
            if(log.size() == 0){
                return -1;
            }

            return last().getIndex();
        }

        @Override public long lastTerm() {
            if(log.size() == 0){
                return -1;
            }

            return last().getTerm();
        }

        @Override public void removeFrom(long index) {
            if(index >= log.size() || index < 0){
                return;
            }

            log.subList((int) index, log.size()).clear();
            //log.remove((int) index);
        }

        @Override public void removeFromAndPersist(long index) {
            removeFrom(index);
        }

        @Override public void append(ReplicatedLogEntry replicatedLogEntry) {
            log.add(replicatedLogEntry);
        }

        @Override public void appendAndPersist(
            ReplicatedLogEntry replicatedLogEntry) {
            append(replicatedLogEntry);
        }

        @Override public List<ReplicatedLogEntry> getFrom(long index) {
            if(index >= log.size() || index < 0){
                return Collections.EMPTY_LIST;
            }
            List<ReplicatedLogEntry> entries = new ArrayList<>();
            for(int i=(int) index ; i < log.size() ; i++) {
                entries.add(get(i));
            }
            return entries;
        }

        @Override
        public List<ReplicatedLogEntry> getFrom(long logEntryIndex, int max) {
            int adjustedIndex = adjustedIndex(logEntryIndex);
            int size = log.size();
            List<ReplicatedLogEntry> entries = new ArrayList<>(100);
            if (adjustedIndex >= 0 && adjustedIndex < size) {
                // physical index should be less than list size and >= 0
                int maxIndex = adjustedIndex + max;
                if(maxIndex > size){
                    maxIndex = size;
                }
                entries.addAll(log.subList(adjustedIndex, maxIndex));
            }
            return entries;
        }

        @Override public long size() {
            return log.size();
        }

        @Override  public boolean isPresent(long logEntryIndex) {
            if (logEntryIndex > lastIndex()) {
                // if the request logical index is less than the last present in the list
                return false;
            }
            int adjustedIndex = adjustedIndex(logEntryIndex);
            return (adjustedIndex >= 0);
        }

        @Override public boolean isInSnapshot(long index) {
            return false;
        }

        @Override public ByteString getSnapshot() {
            return null;
        }

        @Override public long getSnapshotIndex() {
            return -1;
        }

        @Override public long getSnapshotTerm() {
            return -1;
        }

        @Override
        public void setSnapshotTerm(long snapshotTerm) {
        }

        @Override
        public void setSnapshot(ByteString snapshot) {
        }

        @Override
        public void clear(int startIndex, int endIndex) {
            log.subList(startIndex, endIndex).clear();
        }

        @Override
        public void setSnapshotIndex(long snapshotIndex) {
            this.snapshotIndex = snapshotIndex;
        }
    }

    public static class MockPayload extends Payload implements Serializable {
        private String value = "";

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

        @Override public String getClientPayloadClassName() {
            return MockPayload.class.getName();
        }

        public String toString() {
            return value;
        }
    }

    public static class MockReplicatedLogEntry implements ReplicatedLogEntry, Serializable {

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
    }
}
