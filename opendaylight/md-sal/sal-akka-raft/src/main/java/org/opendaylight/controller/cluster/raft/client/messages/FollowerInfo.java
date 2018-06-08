/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.client.messages;

import java.beans.ConstructorProperties;

/**
 * A bean class containing a snapshot of information for a follower returned from GetOnDemandRaftStats.
 *
 * @author Thomas Pantelis
 */
public class FollowerInfo {
    private final String id;
    private final long nextIndex;
    private final long matchIndex;
    private final boolean isActive;
    private final String timeSinceLastActivity;
    private final boolean isVoting;

    @ConstructorProperties({"id","nextIndex", "matchIndex", "active", "timeSinceLastActivity", "voting"})
    public FollowerInfo(String id, long nextIndex, long matchIndex, boolean active, String timeSinceLastActivity,
            boolean voting) {
        this.id = id;
        this.nextIndex = nextIndex;
        this.matchIndex = matchIndex;
        this.isActive = active;
        this.timeSinceLastActivity = timeSinceLastActivity;
        this.isVoting = voting;
    }

    public String getId() {
        return id;
    }

    public long getNextIndex() {
        return nextIndex;
    }

    public long getMatchIndex() {
        return matchIndex;
    }

    public boolean isActive() {
        return isActive;
    }

    public String getTimeSinceLastActivity() {
        return timeSinceLastActivity;
    }

    public boolean isVoting() {
        return isVoting;
    }
}
