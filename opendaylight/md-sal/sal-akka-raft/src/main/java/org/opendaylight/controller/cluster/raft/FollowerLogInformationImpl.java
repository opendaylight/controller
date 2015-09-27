/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import com.google.common.base.Stopwatch;
import java.util.concurrent.TimeUnit;

public class FollowerLogInformationImpl implements FollowerLogInformation {
    private final String id;

    private final Stopwatch stopwatch = Stopwatch.createUnstarted();

    private final RaftActorContext context;

    private long nextIndex;

    private long matchIndex;

    private long lastReplicatedIndex = -1L;

    private final Stopwatch lastReplicatedStopwatch = Stopwatch.createUnstarted();

    private short payloadVersion = -1;

    private FollowerState state = FollowerState.VOTING;

    public FollowerLogInformationImpl(String id, long matchIndex, RaftActorContext context) {
        this.id = id;
        this.nextIndex = context.getCommitIndex();
        this.matchIndex = matchIndex;
        this.context = context;
    }

    @Override
    public long incrNextIndex() {
        return nextIndex++;
    }

    @Override
    public long decrNextIndex() {
        return nextIndex--;
    }

    @Override
    public boolean setNextIndex(long nextIndex) {
        if(this.nextIndex != nextIndex) {
            this.nextIndex = nextIndex;
            return true;
        }

        return false;
    }

    @Override
    public long incrMatchIndex(){
        return matchIndex++;
    }

    @Override
    public boolean setMatchIndex(long matchIndex) {
        if(this.matchIndex != matchIndex) {
            this.matchIndex = matchIndex;
            return true;
        }

        return false;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public long getNextIndex() {
        return nextIndex;
    }

    @Override
    public long getMatchIndex() {
        return matchIndex;
    }

    @Override
    public boolean isFollowerActive() {
        if(state == FollowerState.VOTING_NOT_INITIALIZED) {
            return false;
        }

        long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        return (stopwatch.isRunning()) &&
                (elapsed <= context.getConfigParams().getElectionTimeOutInterval().toMillis());
    }

    @Override
    public void markFollowerActive() {
        if (stopwatch.isRunning()) {
            stopwatch.reset();
        }
        stopwatch.start();
    }

    @Override
    public void markFollowerInActive() {
        if (stopwatch.isRunning()) {
            stopwatch.stop();
        }
    }

    @Override
    public long timeSinceLastActivity() {
        return stopwatch.elapsed(TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean okToReplicate() {
        if(state == FollowerState.VOTING_NOT_INITIALIZED) {
            return false;
        }

        // Return false if we are trying to send duplicate data before the heartbeat interval
        if(getNextIndex() == lastReplicatedIndex){
            if(lastReplicatedStopwatch.elapsed(TimeUnit.MILLISECONDS) < context.getConfigParams()
                    .getHeartBeatInterval().toMillis()){
                return false;
            }
        }

        resetLastReplicated();
        return true;
    }

    private void resetLastReplicated(){
        lastReplicatedIndex = getNextIndex();
        if(lastReplicatedStopwatch.isRunning()){
            lastReplicatedStopwatch.reset();
        }
        lastReplicatedStopwatch.start();
    }

    //    @Override
    //    public String toString() {
    //        StringBuilder builder = new StringBuilder();
    //        builder.append("FollowerLogInformationImpl [id=").append(id).append(", nextIndex=").append(nextIndex)
    //                .append(", matchIndex=").append(matchIndex).append(", stopwatch=")
    //                .append(stopwatch.elapsed(TimeUnit.MILLISECONDS))
    //                .append(", followerTimeoutMillis=")
    //                .append(context.getConfigParams().getElectionTimeOutInterval().toMillis()).append("]");
    //        return builder.toString();
    //    }

    @Override
    public short getPayloadVersion() {
        return payloadVersion;
    }

    @Override
    public void setPayloadVersion(short payloadVersion) {
        this.payloadVersion = payloadVersion;
    }

    @Override
    public boolean canParticipateInConsensus() {
        return state == FollowerState.VOTING;
    }

    @Override
    public void setFollowerState(FollowerState state) {
        this.state = state;
    }

    @Override
    public FollowerState getFollowerState() {
        return state;
    }

    @Override
    public String toString() {
        return "FollowerLogInformationImpl [id=" + id + ", nextIndex=" + nextIndex + ", matchIndex=" + matchIndex
                + ", lastReplicatedIndex=" + lastReplicatedIndex + ", state=" + state + ", stopwatch="
                + stopwatch.elapsed(TimeUnit.MILLISECONDS) + ", followerTimeoutMillis="
                + context.getConfigParams().getElectionTimeOutInterval().toMillis() + "]";
    }
}
