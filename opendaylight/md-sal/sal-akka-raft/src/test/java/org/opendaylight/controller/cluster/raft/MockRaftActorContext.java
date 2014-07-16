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

        electionTerm = new ElectionTermImpl(id);

        initReplicatedLog();
    }


    public void initReplicatedLog(){
        this.replicatedLog = new SimpleReplicatedLog();
        this.replicatedLog.append(new MockReplicatedLogEntry(1, 1, ""));
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

    public void setPeerAddresses(Map<String, String> peerAddresses) {
        this.peerAddresses = peerAddresses;
    }


    public static class SimpleReplicatedLog implements ReplicatedLog {
        private final List<ReplicatedLogEntry> log = new ArrayList<>(10000);

        @Override public ReplicatedLogEntry get(long index) {
            if(index >= log.size() || index < 0){
                return null;
            }
            return log.get((int) index);
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
            for(int i=(int) index ; i < log.size() ; i++) {
                log.remove(i);
            }
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

        @Override public long size() {
            return log.size();
        }

        @Override public boolean isPresent(long index) {
            if(index >= log.size() || index < 0){
                return false;
            }

            return true;
        }

        @Override public boolean isInSnapshot(long index) {
            return false;
        }

        @Override public Object getSnapshot() {
            return null;
        }

        @Override public long getSnapshotIndex() {
            return -1;
        }

        @Override public long getSnapshotTerm() {
            return -1;
        }
    }

    public static class MockReplicatedLogEntry implements ReplicatedLogEntry {

        private final long term;
        private final long index;
        private final Object data;

        public MockReplicatedLogEntry(long term, long index, Object data){

            this.term = term;
            this.index = index;
            this.data = data;
        }

        @Override public Object getData() {
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
