/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.state;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.journal.AppendJournal;

@NonNullByDefault
public record TestPreLeaderState(LeaderStuff stuff) implements PreLeaderState {
    public TestPreLeaderState {
        requireNonNull(stuff);
    }

    @Override
    public final short payloadVersion() {
        return stuff.payloadVersion;
    }

    @Override
    public CandidateState toCandidate() {
        return new TestCandidateState(new InactiveStuff(stuff));
    }

    @Override
    public FollowerState toFollower() {
        return new TestFollowerState(new FollowerStuff(stuff));
    }

    @Override
    public IsolatedLeaderState toIsolatedLeader() {
        return new TestIsolatedLeaderState(stuff);
    }

    @Override
    public LeaderState toLeader() {
        return new TestLeaderState(stuff);
    }

    @Override
    public InactiveState toInactive() {
        return TestInactive.INSTANCE;
    }

    @Override
    public void onConsensusAchieved(final AppendJournal request) throws LeaderStateException {
        stuff.onConsensusAchieved(request);
    }

    @Override
    public StateSnapshot takeSnapshot() {
        return stuff.takeSnapshot();
    }
}
