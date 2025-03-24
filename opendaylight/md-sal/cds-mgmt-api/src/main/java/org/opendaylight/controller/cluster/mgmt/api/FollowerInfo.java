/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.mgmt.api;

import static java.util.Objects.requireNonNull;

import javax.management.ConstructorParameters;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A bean class containing a snapshot of information for a follower returned from GetOnDemandRaftStats.
 *
 * @author Thomas Pantelis
 */
// FIXME: this definitely should be in 'raft.api'
// FIXME: should be a record, really, but do not commit to Serializable just yet
@NonNullByDefault
public final class FollowerInfo {
    // FIXME: memberId/serverId ?
    private final String id;
    // FIXME: uint64 safety vs. JMX mapping? Uint64? raft.api.JournalIndex?
    private final long nextIndex;
    private final long matchIndex;
    private final boolean isActive;
    // FIXME: can this be a java.time.Duration?
    private final String timeSinceLastActivity;
    // FIXME: document this one, as =false is an topic which requires a documentation link to FAQ/etc.
    //        it certainly deserves to be in a separate structure.
    private final boolean isVoting;

    @ConstructorParameters({"id","nextIndex", "matchIndex", "active", "timeSinceLastActivity", "voting"})
    public FollowerInfo(final String id, final long nextIndex, final long matchIndex, final boolean active,
            final String timeSinceLastActivity, final boolean voting) {
        this.id = requireNonNull(id);
        this.nextIndex = nextIndex;
        this.matchIndex = matchIndex;
        isActive = active;
        this.timeSinceLastActivity = timeSinceLastActivity;
        isVoting = voting;
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
