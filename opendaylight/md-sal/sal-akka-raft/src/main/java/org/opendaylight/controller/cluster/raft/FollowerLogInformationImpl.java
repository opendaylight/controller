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
import java.util.concurrent.atomic.AtomicLong;
import scala.concurrent.duration.FiniteDuration;

public class FollowerLogInformationImpl implements FollowerLogInformation{

    private final String id;

    private final AtomicLong nextIndex;

    private final AtomicLong matchIndex;

    private final Stopwatch stopwatch;

    private final RaftActorContext context;

    private final long followerTimeoutMillis;

    private long lastReplicatedIndex = -1L;

    private final Stopwatch lastReplicatedStopwatch = new Stopwatch();

    public FollowerLogInformationImpl(String id, AtomicLong nextIndex,
        AtomicLong matchIndex, FiniteDuration followerTimeoutDuration, RaftActorContext context) {
        this.id = id;
        this.nextIndex = nextIndex;
        this.matchIndex = matchIndex;
        this.stopwatch = new Stopwatch();
        this.followerTimeoutMillis = followerTimeoutDuration.toMillis();
        this.context = context;
    }

    @Override
    public long incrNextIndex(){
        return nextIndex.incrementAndGet();
    }

    @Override public long decrNextIndex() {
        return nextIndex.decrementAndGet();
    }

    @Override public void setNextIndex(long nextIndex) {
        this.nextIndex.set(nextIndex);
    }

    @Override
    public long incrMatchIndex(){
        return matchIndex.incrementAndGet();
    }

    @Override public void setMatchIndex(long matchIndex) {
        this.matchIndex.set(matchIndex);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public AtomicLong getNextIndex() {
        return nextIndex;
    }

    @Override
    public AtomicLong getMatchIndex() {
        return matchIndex;
    }

    @Override
    public boolean isFollowerActive() {
        long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        return (stopwatch.isRunning()) && (elapsed <= followerTimeoutMillis);
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
        // Return false if we are trying to send duplicate data before the heartbeat interval
        if(getNextIndex().longValue() == lastReplicatedIndex){
            if(lastReplicatedStopwatch.elapsed(TimeUnit.MILLISECONDS) < context.getConfigParams()
                    .getHeartBeatInterval().toMillis()){
                return false;
            }
        }

        resetLastReplicated();
        return true;
    }

    private void resetLastReplicated(){
        lastReplicatedIndex = getNextIndex().longValue();
        if(lastReplicatedStopwatch.isRunning()){
            lastReplicatedStopwatch.reset();
        }
        lastReplicatedStopwatch.start();
    }
}
