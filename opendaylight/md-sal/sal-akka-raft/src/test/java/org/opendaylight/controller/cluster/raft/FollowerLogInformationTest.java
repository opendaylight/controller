/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Uninterruptibles;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class FollowerLogInformationTest {

    @Test
    public void testIsFollowerActive() {

        MockRaftActorContext context = new MockRaftActorContext();
        context.setCommitIndex(10);

        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(Duration.ofMillis(500));
        configParams.setElectionTimeoutFactor(1);
        context.setConfigParams(configParams);

        FollowerLogInformation followerLogInformation =
                new FollowerLogInformation(new PeerInfo("follower1", null, VotingState.VOTING), 9, context);

        assertFalse("Follower should be termed inactive before stopwatch starts",
                followerLogInformation.isFollowerActive());

        followerLogInformation.markFollowerActive();
        if (sleepWithElaspsedTimeReturned(200) > 200) {
            return;
        }
        assertTrue("Follower should be active", followerLogInformation.isFollowerActive());

        if (sleepWithElaspsedTimeReturned(400) > 400) {
            return;
        }
        assertFalse("Follower should be inactive after time lapsed",
                followerLogInformation.isFollowerActive());

        followerLogInformation.markFollowerActive();
        assertTrue("Follower should be active from inactive",
                followerLogInformation.isFollowerActive());
    }

    // we cannot rely comfortably that the sleep will indeed sleep for the desired time
    // hence getting the actual elapsed time and do a match.
    // if the sleep has spilled over, then return the test gracefully
    private static long sleepWithElaspsedTimeReturned(final long millis) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        Uninterruptibles.sleepUninterruptibly(millis, TimeUnit.MILLISECONDS);
        stopwatch.stop();
        return stopwatch.elapsed(TimeUnit.MILLISECONDS);
    }

    @Test
    public void testOkToReplicate() {
        MockRaftActorContext context = new MockRaftActorContext();
        context.setCommitIndex(0);
        FollowerLogInformation followerLogInformation =
                new FollowerLogInformation(new PeerInfo("follower1", null, VotingState.VOTING), 10, context);

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
    public void testVotingNotInitializedState() {
        final PeerInfo peerInfo = new PeerInfo("follower1", null, VotingState.VOTING_NOT_INITIALIZED);
        MockRaftActorContext context = new MockRaftActorContext();
        context.setCommitIndex(0);
        FollowerLogInformation followerLogInformation = new FollowerLogInformation(peerInfo, context);

        assertFalse(followerLogInformation.okToReplicate(0));

        followerLogInformation.markFollowerActive();
        assertFalse(followerLogInformation.isFollowerActive());

        peerInfo.setVotingState(VotingState.VOTING);
        assertTrue(followerLogInformation.okToReplicate(0));

        followerLogInformation.markFollowerActive();
        assertTrue(followerLogInformation.isFollowerActive());
    }

    @Test
    public void testNonVotingState() {
        final PeerInfo peerInfo = new PeerInfo("follower1", null, VotingState.NON_VOTING);
        MockRaftActorContext context = new MockRaftActorContext();
        context.setCommitIndex(0);
        FollowerLogInformation followerLogInformation = new FollowerLogInformation(peerInfo, context);

        assertTrue(followerLogInformation.okToReplicate(0));

        followerLogInformation.markFollowerActive();
        assertTrue(followerLogInformation.isFollowerActive());
    }

    @Test
    public void testDecrNextIndex() {
        MockRaftActorContext context = new MockRaftActorContext();
        context.setCommitIndex(1);
        FollowerLogInformation followerLogInformation =
                new FollowerLogInformation(new PeerInfo("follower1", null, VotingState.VOTING), 1, context);

        assertTrue(followerLogInformation.decrNextIndex(1));
        assertEquals("getNextIndex", 0, followerLogInformation.getNextIndex());

        assertTrue(followerLogInformation.decrNextIndex(1));
        assertEquals("getNextIndex", -1, followerLogInformation.getNextIndex());

        assertFalse(followerLogInformation.decrNextIndex(1));
        assertEquals("getNextIndex", -1, followerLogInformation.getNextIndex());
    }
}
