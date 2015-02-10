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
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import scala.concurrent.duration.FiniteDuration;

public class FollowerLogInformationImpl implements FollowerLogInformation {
    private static final AtomicLongFieldUpdater<FollowerLogInformationImpl> NEXT_INDEX_UPDATER = AtomicLongFieldUpdater.newUpdater(FollowerLogInformationImpl.class, "nextIndex");
    private static final AtomicLongFieldUpdater<FollowerLogInformationImpl> MATCH_INDEX_UPDATER = AtomicLongFieldUpdater.newUpdater(FollowerLogInformationImpl.class, "matchIndex");

    private final String id;

    private final Stopwatch stopwatch = new Stopwatch();

    private final long followerTimeoutMillis;

    private volatile long nextIndex;

    private volatile long matchIndex;

    public FollowerLogInformationImpl(String id, long nextIndex,
        long matchIndex, FiniteDuration followerTimeoutDuration) {
        this.id = id;
        this.nextIndex = nextIndex;
        this.matchIndex = matchIndex;
        this.followerTimeoutMillis = followerTimeoutDuration.toMillis();
    }

    @Override
    public long incrNextIndex(){
        return NEXT_INDEX_UPDATER.incrementAndGet(this);
    }

    @Override
    public long decrNextIndex() {
        return NEXT_INDEX_UPDATER.decrementAndGet(this);
    }

    @Override
    public void setNextIndex(long nextIndex) {
        this.nextIndex = nextIndex;
    }

    @Override
    public long incrMatchIndex(){
        return MATCH_INDEX_UPDATER.incrementAndGet(this);
    }

    @Override
    public void setMatchIndex(long matchIndex) {
        this.matchIndex = matchIndex;
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
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FollowerLogInformationImpl [id=").append(id).append(", nextIndex=").append(nextIndex)
                .append(", matchIndex=").append(matchIndex).append(", stopwatch=")
                .append(stopwatch.elapsed(TimeUnit.MILLISECONDS))
                .append(", followerTimeoutMillis=").append(followerTimeoutMillis).append("]");
        return builder.toString();
    }


}
