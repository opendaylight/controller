/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Uninterruptibles;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FollowerLogInformationTest {
    @TempDir
    public Path stateDir;

    private MockRaftActorContext context;

    @BeforeEach
    public void beforeEach() {
        context = new MockRaftActorContext(stateDir);
    }

    @Test
    void testIsFollowerActive() {
        context.getReplicatedLog().setCommitIndex(10);

        final var configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(Duration.ofMillis(500));
        configParams.setElectionTimeoutFactor(1);
        context.setConfigParams(configParams);

        final var followerLogInformation =
                new FollowerLogInformation(new PeerInfo("follower1", null, VotingState.VOTING), 9, context);

        assertFalse(followerLogInformation.isFollowerActive());

        followerLogInformation.markFollowerActive();
        if (sleepWithElaspsedTimeReturned(200) > 200) {
            // FIXME: what?!
            return;
        }
        assertTrue(followerLogInformation.isFollowerActive());

        if (sleepWithElaspsedTimeReturned(400) > 400) {
            // FIXME: what?!
            return;
        }
        // Follower should be inactive after time lapsed
        assertFalse(followerLogInformation.isFollowerActive());

        followerLogInformation.markFollowerActive();
        // Follower should be active from inactive
        assertTrue(followerLogInformation.isFollowerActive());
    }

    // we cannot rely comfortably that the sleep will indeed sleep for the desired time
    // hence getting the actual elapsed time and do a match.
    // if the sleep has spilled over, then return the test gracefully
    private static long sleepWithElaspsedTimeReturned(final long millis) {
        final var stopwatch = Stopwatch.createStarted();
        Uninterruptibles.sleepUninterruptibly(millis, TimeUnit.MILLISECONDS);
        stopwatch.stop();
        return stopwatch.elapsed(TimeUnit.MILLISECONDS);
    }

    @Test
    void testOkToReplicate() {
        context.getReplicatedLog().setCommitIndex(0);
        final var followerLogInformation = new FollowerLogInformation(
            new PeerInfo("follower1", null, VotingState.VOTING), 10, context);

        followerLogInformation.setSentCommitIndex(0);
        assertTrue(followerLogInformation.okToReplicate(0));
        assertFalse(followerLogInformation.okToReplicate(0));

        // wait for 150 milliseconds and it should work again
        Uninterruptibles.sleepUninterruptibly(150, TimeUnit.MILLISECONDS);
        assertTrue(followerLogInformation.okToReplicate(0));

        //increment next index and try immediately and it should work again
        followerLogInformation.incrNextIndex();
        assertTrue(followerLogInformation.okToReplicate(0));
    }

    @Test
    void testVotingNotInitializedState() {
        final var peerInfo = new PeerInfo("follower1", null, VotingState.VOTING_NOT_INITIALIZED);
        context.getReplicatedLog().setCommitIndex(0);
        final var followerLogInformation = new FollowerLogInformation(peerInfo, context);

        assertFalse(followerLogInformation.okToReplicate(0));

        followerLogInformation.markFollowerActive();
        assertFalse(followerLogInformation.isFollowerActive());

        peerInfo.setVotingState(VotingState.VOTING);
        assertTrue(followerLogInformation.okToReplicate(0));

        followerLogInformation.markFollowerActive();
        assertTrue(followerLogInformation.isFollowerActive());
    }

    @Test
    void testNonVotingState() {
        final var peerInfo = new PeerInfo("follower1", null, VotingState.NON_VOTING);
        context.getReplicatedLog().setCommitIndex(0);
        final var followerLogInformation = new FollowerLogInformation(peerInfo, context);

        assertTrue(followerLogInformation.okToReplicate(0));

        followerLogInformation.markFollowerActive();
        assertTrue(followerLogInformation.isFollowerActive());
    }

    @Test
    void testDecrNextIndex() {
        context.getReplicatedLog().setCommitIndex(1);
        final var followerLogInformation = new FollowerLogInformation(
            new PeerInfo("follower1", null, VotingState.VOTING), 1, context);

        assertTrue(followerLogInformation.decrNextIndex(1));
        assertEquals(0, followerLogInformation.getNextIndex());

        assertTrue(followerLogInformation.decrNextIndex(1));
        assertEquals(-1, followerLogInformation.getNextIndex());

        assertFalse(followerLogInformation.decrNextIndex(1));
        assertEquals(-1, followerLogInformation.getNextIndex());
    }
}
